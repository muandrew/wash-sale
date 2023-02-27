use crate::shares::StockBin;
use crate::{money::Thou, shares::StockBinType};
use chrono::{prelude::*, Duration};
use serde::{
    de::{self, Visitor},
    Deserialize,
};
use std::collections::BTreeMap;
use std::{collections::HashMap, error::Error, fs::File, io::BufReader, ops::Deref, usize};

pub struct TheWorld {
    date_counter: HashMap<NaiveDate, usize>,
    bins: BTreeMap<TimeKey, StockBin>,
}

#[derive(Copy, Clone)]
struct TimeKeyStockBin(TimeKey, StockBin);

impl TheWorld {
    pub fn new() -> TheWorld {
        TheWorld {
            date_counter: HashMap::new(),
            bins: BTreeMap::new(),
        }
    }

    pub fn print_world(&self) {
        self.bins
            .iter()
            .for_each(|(k, v)| println!("time: {k:?} content: {v:?}"))
    }

    fn add_bin_and_inc_tk_counter(&mut self, bin: StockBin) {
        let counter = match self.date_counter.get(&bin.date) {
            Some(x) => x,
            None => &0,
        };
        let counter = *counter;
        self.date_counter.insert(bin.date, counter + 1);
        self.bins.insert(TimeKey(bin.date, counter), bin);
    }

    fn accept_purchase_or_release(&mut self, event: Event) -> &str {
        // claimed losses may be annihilated by this new release
        let wash_sale = self.filter_last_30(event.date.0, StockBinType::CLAIMED_LOSS);
        if wash_sale.len() == 0 {
            self.add_bin_and_inc_tk_counter(StockBin {
                bin_type: StockBinType::AVAILABLE,
                date: event.date.0,
                generation: 0,
                shares: event.shares,
                cost_basis: event.transaction_amount,
            });
            "got some stocks!"
        } else {
            "wash sale calculations!"
        }
    }

    // dont need to check after because processing in order, will check when sale or release occurs
    fn filter_last_30(&self, date: NaiveDate, bin_type: StockBinType) -> Vec<TimeKeyStockBin> {
        let before = date - Duration::days(30);
        self
            .bins
            .iter()
            .filter(|(time, bin)| {
                let date = time.0;
                date >= before && bin.bin_type == bin_type
            })
            .map(|(k, v)| TimeKeyStockBin(*k, *v))
            .collect()
    }

    pub fn accept_event(&mut self, event: Event) -> &str {
        match event.event_type {
            EventType::RELEASE => self.accept_purchase_or_release(event),
            EventType::SALE => {
                let wash_sale = self.filter_last_30(event.date.0, StockBinType::AVAILABLE);
                if wash_sale.len() > 0 {
                    let mut new_bins: Vec<StockBin> = vec![];
                    let mut shares_to_consume = event.shares;
                    for time_bin in wash_sale {
                        let bin1 = time_bin.1;
                        // everything will be consumed
                        if bin1.shares <= shares_to_consume {
                            let mut bin2 = bin1.clone();
                            bin2.cost_basis = bin1
                                .cost_basis
                                // TODO: double check math
                                .add(&event.market_price.multiply(event.shares));
                            bin2.bin_type = StockBinType::REPLACEMENT;
                            // remove bin, should exist so unwrap should be ok
                            self.bins.remove(&time_bin.0).unwrap();
                            new_bins.push(bin2);
                            shares_to_consume = shares_to_consume - time_bin.1.shares;
                        // some leftovers to break out
                        } else {
                            let bin1 = time_bin.1;

                            let mut bin1leftover = bin1;
                            bin1leftover.shares = bin1.shares - shares_to_consume;

                            let mut bin2 = bin1;
                            bin2.shares = shares_to_consume;
                            // TODO: double check math
                            bin2.cost_basis = bin1
                                .cost_basis
                                .add(&event.market_price.multiply(event.shares));
                            bin2.bin_type = StockBinType::REPLACEMENT;

                            // replace bin now since we don't want to calculate a new number for it
                            self.bins.insert(time_bin.0, bin1leftover);
                            new_bins.push(bin2);
                            shares_to_consume = 0
                        }
                        if shares_to_consume <= 0 {
                            break;
                        }
                    }
                    for new_bin in new_bins {
                        self.add_bin_and_inc_tk_counter(new_bin)
                    }
                    if shares_to_consume > 0 {
                        self.add_bin_and_inc_tk_counter(StockBin {
                            bin_type: StockBinType::CLAIMED_LOSS,
                            date: event.date.0,
                            generation: 0,
                            shares: shares_to_consume,
                            cost_basis: event.market_price.multiply(shares_to_consume),
                        });
                        "some losses allowed"
                    } else {
                        "all wash sale disallowed"
                    }
                } else {
                    self.add_bin_and_inc_tk_counter(StockBin {
                        bin_type: StockBinType::CLAIMED_LOSS,
                        date: event.date.0,
                        generation: 0,
                        shares: event.shares,
                        cost_basis: event.transaction_amount,
                    });
                    "i sold something!"
                }
            }
            EventType::PURCHASE => self.accept_purchase_or_release(event),
        }
    }
}

pub fn example(file_path: &str) -> Result<(), Box<dyn Error>> {
    let file = File::open(file_path)?;
    let buf_reader = BufReader::new(file);

    let mut rdr = csv::Reader::from_reader(buf_reader);

    let mut world = TheWorld::new();
    for result in rdr.deserialize() {
        // Notice that we need to provide a type hint for automatic
        // deserialization.
        let record: Event = result?;
        let msg = world.accept_event(record);
        println!("{}", msg);
    }
    world.print_world();
    return Ok(());
}

#[derive(Copy, Clone, Debug, PartialEq, Eq, Ord, PartialOrd)]
struct TimeKey(NaiveDate, usize);

#[derive(Debug)]
pub struct ND(NaiveDate);

impl Deref for ND {
    type Target = NaiveDate;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

// from the perspective of my account
#[derive(Debug, Deserialize)]
pub enum EventType {
    RELEASE,
    SALE,
    PURCHASE,
}

#[derive(Debug, Deserialize)]
pub struct Event {
    batch_id: String,
    date: ND,
    event_type: EventType,
    market_price: Thou, // used to compute gain / loss
    shares: usize,
    transaction_amount: Thou, // how much the transaction was for
}

impl Event {
    /// compute how much each share was worth
    fn value_per_share(&self) -> (Thou, Thou) {
        (
            Thou(self.transaction_amount.0 / self.shares),
            Thou(self.transaction_amount.0 % self.shares),
        )
    }

    /// total gains
    fn total_gains(&self) -> Thou {
        Thou(self.transaction_amount.0 - (self.market_price.0 * self.shares))
    }
}

impl<'de> Deserialize<'de> for ND {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        struct TimestampVisitor;

        impl<'de> Visitor<'de> for TimestampVisitor {
            type Value = ND;

            fn expecting(&self, formatter: &mut std::fmt::Formatter) -> std::fmt::Result {
                formatter.write_str("Timestamp in RFC3339 format")
            }

            fn visit_str<E>(self, value: &str) -> Result<Self::Value, E>
            where
                E: de::Error,
            {
                match NaiveDate::parse_from_str(value, "%Y-%m-%d") {
                    Ok(a) => Ok(ND(a)),
                    Err(e) => Err(de::Error::custom("no clue")),
                }
            }
        }
        deserializer.deserialize_str(TimestampVisitor)
    }
}

pub fn naive_data_parse_ymd(input: &str) -> NaiveDate {
    NaiveDate::parse_from_str(input, "%Y-%m-%d").unwrap()
}

#[cfg(test)]
mod tests {
    use std::cmp::Ordering;

    use crate::timeline::ND;

    use super::{naive_data_parse_ymd, Event, EventType, Thou, TimeKey};
    use chrono::Duration;

    #[test]
    fn test_value_per_share() {
        let event = Event {
            batch_id: "some_id".to_string(),
            date: ND(naive_data_parse_ymd("2015-09-05")),
            event_type: EventType::RELEASE,
            market_price: Thou::from_dollars(1),
            shares: 5,
            transaction_amount: Thou::from_dollars(100),
        };

        let (vps, extra) = event.value_per_share();
        assert_eq!(vps, Thou::from_dollars(20));
        assert_eq!(extra.0, 0);

        let gains = event.total_gains();
        assert_eq!(gains, Thou::from_dollars(95));
    }

    #[test]
    fn test_days() {
        let date = naive_data_parse_ymd("2024-02-28");
        let after = date + Duration::days(30);
        assert_eq!(after, date);
    }

    #[test]
    fn test_order_when_date_same() {
        let a = TimeKey(naive_data_parse_ymd("2024-02-28"), 1);
        let b = TimeKey(naive_data_parse_ymd("2024-02-28"), 2);
        let res = a.cmp(&b);
        assert_eq!(Ordering::Less, res);
    }

    #[test]
    fn test_order_when_date_different() {
        let a = TimeKey(naive_data_parse_ymd("2024-02-28"), 1);
        let b = TimeKey(naive_data_parse_ymd("2024-01-28"), 2);
        let res = a.cmp(&b);
        assert_eq!(Ordering::Greater, res);
    }
}

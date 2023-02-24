use crate::money::Thou;
use crate::shares::StockBin;
use chrono::{prelude::*, Duration};
use serde::{
    de::{self, Visitor},
    Deserialize,
};
use std::collections::BTreeMap;
use std::{collections::HashMap, error::Error, fs::File, io::BufReader, ops::Deref, usize};

pub struct TheWorld {
    date_counter: HashMap<NaiveDate, usize>,
    bins2: BTreeMap<TimeKey, StockBin>,
}

#[derive(Copy, Clone)]
struct Cheat(TimeKey, StockBin);

impl TheWorld {
    pub fn new() -> TheWorld {
        TheWorld {
            date_counter: HashMap::new(),
            bins2: BTreeMap::new(),
        }
    }

    pub fn print_world(&self) {
        self.bins2
            .iter()
            .for_each(|(k, v)| println!("time: {k:?} content: {v:?}"))
    }

    fn accept_purchase_or_release(&mut self, event: Event) -> &str {
        let counter = match self.date_counter.get(&event.date.0) {
            Some(x) => x,
            None => &0,
        };
        let counter = *counter;
        self.date_counter.insert(event.date.0, counter + 1);
        self.bins2.insert(
            TimeKey(event.date.0, counter),
            StockBin {
                date: event.date.0,
                generation: 0,
                shares: event.shares,
                cost_basis: event.transaction_amount,
                is_replacement: false,
            },
        );
        "got some stocks!"
    }

    fn addBinAndIncrementTimeKeyCounter(&mut self, bin: StockBin) {
        let counter = match self.date_counter.get(&bin.date) {
            Some(x) => x,
            None => &0,
        };
        let counter = *counter;
        self.date_counter.insert(bin.date, counter + 1);
        self.bins2.insert(TimeKey(bin.date, counter), bin);
    }

    pub fn accept_event(&mut self, event: Event) -> &str {
        match event.event_type {
            EventType::RELEASE => self.accept_purchase_or_release(event),
            EventType::SALE => {
                let before = event.date.0 - Duration::days(30);
                let after = event.date.0 + Duration::days(30);
                let wash_sale: Vec<Cheat> = self
                    .bins2
                    .iter()
                    .filter(|(time, bin)| {
                        let date = time.0;
                        date >= before && date <= after && !bin.is_replacement
                    })
                    .map(|(k, v)| Cheat(*k, *v))
                    .collect();
                if wash_sale.len() > 0 {
                    let mut new_bins: Vec<StockBin> = vec![];
                    let mut shares_to_consume = event.shares;
                    for bin in wash_sale {
                        // everything will be consumed
                        if bin.1.shares <= shares_to_consume {
                            let bin2 = StockBin {
                                date: bin.1.date,
                                generation: bin.1.generation,
                                shares: bin.1.shares,
                                cost_basis: bin
                                    .1
                                    .cost_basis
                                    // TODO: double check math
                                    .add(&event.market_price.multiply(event.shares)),
                                is_replacement: true,
                            };
                            // remove bin, should exist so unwrap should be ok
                            self.bins2.remove(&bin.0).unwrap();
                            new_bins.push(bin2);
                            shares_to_consume = shares_to_consume - bin.1.shares;
                        // some leftovers to break out
                        } else {
                            let bin1 = StockBin {
                                date: bin.1.date,
                                generation: bin.1.generation,
                                shares: bin.1.shares - shares_to_consume,
                                // left over not consumed, so the same
                                cost_basis: bin.1.cost_basis,
                                is_replacement: false,
                            };
                            let bin2 = StockBin {
                                date: bin.1.date,
                                // watch out for different generations
                                generation: bin.1.generation,
                                shares: shares_to_consume,
                                // TODO: double check math
                                cost_basis: bin
                                    .1
                                    .cost_basis
                                    .add(&event.market_price.multiply(event.shares)),
                                is_replacement: true,
                            };
                            // replace bin now since we don't want to calculate a new number for it
                            self.bins2.insert(bin.0, bin1);
                            new_bins.push(bin2);
                            shares_to_consume = 0
                        }
                        if shares_to_consume <= 0 {
                            break;
                        }
                    }
                    for new_bin in new_bins {
                        self.addBinAndIncrementTimeKeyCounter(new_bin)
                    }
                    if shares_to_consume > 0 {
                        "some losses allowed"
                    } else {
                        "all wash sale disallowed"
                    }
                } else {
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

use chrono::{prelude::*, Duration};
use serde::{
    de::{self, Visitor},
    Deserialize,
};
use std::{error::Error, fmt, fs::File, io::BufReader, usize};

#[derive(Copy, Clone)]
pub struct StockBin {
    date: NaiveDate,
    generation: usize, // tracking stock splits
    shares: usize,
    cost_basis: Thou,
    is_replacement: bool, // true if this was adjusted for replacement
}

pub struct TheWorld {
    bins: Vec<StockBin>,
}

impl TheWorld {
    pub fn new() -> TheWorld {
        TheWorld { bins: vec![] }
    }

    pub fn accept_event(&self, event: Event) -> &str {
        match event.event_type {
            EventType::RELEASE => "i got something!",
            EventType::SALE => {
                let before = event.date.0 - Duration::days(30);
                let after = event.date.0 + Duration::days(30);
                let wash_sale: Vec<StockBin> = self
                    .bins
                    .iter()
                    .filter(|x| x.date >= before || x.date <= after)
                    .copied()
                    .collect();
                if wash_sale.len() > 0 {
                    "wash sale disallowed"
                } else {
                    "i sold something!"  
                } 
            }
            EventType::PURCHASE => "i bought something!",
        }
    }
}

pub fn example(file_path: &str) -> Result<(), Box<dyn Error>> {
    let file = File::open(file_path)?;
    let buf_reader = BufReader::new(file);

    let mut rdr = csv::Reader::from_reader(buf_reader);

    let world = TheWorld::new();
    for result in rdr.deserialize() {
        // Notice that we need to provide a type hint for automatic
        // deserialization.
        let record: Event = result?;
        let msg = world.accept_event(record);
        println!("{}", msg);
    }
    return Ok(());
}

#[derive(Copy, Clone, Debug, Deserialize, PartialEq)]
pub struct Thou(pub usize);

#[derive(Debug)]
pub struct ND(NaiveDate);

impl fmt::Display for Thou {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let dollars = self.0 / THOUS_PER_DOLLAR;
        let cents = self.0 % THOUS_PER_DOLLAR / THOUS_PER_CENT;
        let thous = self.0 % THOUS_PER_CENT;
        write!(f, "${}.{:0>2}_{:0>3}", dollars, cents, thous)
    }
}

const THOUS_PER_DOLLAR: usize = 100_000;
const THOUS_PER_CENT: usize = 1_000;

impl Thou {
    /// Convert from dollars
    ///
    /// ```
    /// use wash_sale::timeline::Thou;
    ///
    /// assert_eq!(format!("{}",Thou::from_dollars(1)),  "$1.00_000");
    /// ```
    pub fn from_dollars(dollars: usize) -> Thou {
        Thou(dollars * THOUS_PER_DOLLAR)
    }

    /// Convert from dollars and cents
    ///
    /// ```
    /// use wash_sale::timeline::Thou;
    ///
    /// assert_eq!(format!("{}", Thou::from_dc(2, 3)), "$2.03_000");
    /// assert_eq!(format!("{}", Thou::from_dc(4, 51)), "$4.51_000");
    /// assert_eq!(format!("{}", Thou::from_dc(5, 101)), "$6.01_000");
    /// ```
    pub fn from_dc(dollars: usize, cents: usize) -> Thou {
        Thou(dollars * THOUS_PER_DOLLAR + cents * THOUS_PER_CENT)
    }

    /// Convert from dollars, cents, and thous
    ///
    /// ```
    /// use wash_sale::timeline::Thou;
    ///
    /// assert_eq!(format!("{}", Thou::from_dct(1, 2, 3)), "$1.02_003");
    /// ```
    pub fn from_dct(dollars: usize, cents: usize, thous: usize) -> Thou {
        Thou(dollars * THOUS_PER_DOLLAR + cents * THOUS_PER_CENT + thous)
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
    use crate::timeline::ND;

    use super::{naive_data_parse_ymd, Event, EventType, Thou};
    use chrono::{Duration};

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
}

use chrono::prelude::*;
use std::{fmt, usize};

pub fn a_fn() {
    println!("test");
}

#[derive(Debug, PartialEq)]
pub struct Thou(pub usize);

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

pub enum EventType {
    RELEASE,
    SALE,
    PURCHASE,
}
pub struct Event {
    batch_id: String,
    date: NaiveDate,
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

#[cfg(test)]
mod tests {
    use super::{Event, EventType, Thou};
    use chrono::prelude::*;

    #[test]
    fn test_value_per_share() {
        let event = Event {
            batch_id: "some_id".to_string(),
            date: NaiveDate::parse_from_str("2015-09-05", "%Y-%m-%d").unwrap(),
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
}

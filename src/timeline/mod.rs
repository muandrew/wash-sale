use chrono::prelude::*;
use std::{fmt, usize};

pub fn a_fn() {
    println!("test");
}

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

enum EventType {
    RELEASE,
    SALE,
    PURCHASE,
}
struct Event {
    date: NaiveDate,
    batch_id: String,
    event_type: EventType,
    shares: usize,
    amount: i64,
    market_price: i64,
}

impl Event {
    fn value_per_share_rounded(&self) -> i64 {
        self.amount / self.market_price;
        return 1;
    }
}

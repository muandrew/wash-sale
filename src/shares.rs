use chrono::NaiveDate;

use crate::money::Thou;

#[derive(Copy, Clone, Debug)]
pub struct StockBin {
    pub bin_type: StockBinType,
    pub date: NaiveDate,
    pub generation: usize, // tracking stock splits
    pub shares: usize,
    pub cost_basis: Thou,
}

#[derive(Copy, Clone, Debug, PartialEq)]
pub enum StockBinType {
    AVAILABLE,
    CLAIMED_LOSS,
    REPLACEMENT,
}

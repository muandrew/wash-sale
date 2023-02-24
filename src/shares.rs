use chrono::NaiveDate;

use crate::money::Thou;

#[derive(Copy, Clone, Debug)]
pub struct StockBin {
    pub date: NaiveDate,
    pub generation: usize, // tracking stock splits
    pub shares: usize,
    pub cost_basis: Thou,
    pub is_replacement: bool, // true if this was adjusted for replacement
}

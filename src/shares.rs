use chrono::NaiveDate;

use crate::money::Thou;


#[derive(Copy, Clone)]
pub struct StockBin {
    pub date: NaiveDate,
    generation: usize, // tracking stock splits
    shares: usize,
    cost_basis: Thou,
    is_replacement: bool, // true if this was adjusted for replacement
}
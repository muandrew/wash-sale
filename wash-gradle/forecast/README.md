# Forecast calculation conventions

`MultiAssetEngine` uses one annual ledger for deterministic projections, Monte
Carlo paths, and household/pool/expense charts. All values are **nominal dollars**.
Income raises are additional to inflation when `inflationAdjusted` is enabled;
fixed nominal income and expenses should disable that flag. Income is treated as
cash available to spend; this module does not calculate income or withdrawal taxes.

Each row shows opening assets and liabilities, followed by that calendar year's
actual funding and pool transfers. `closingNetWorth` equals the next row's opening
net worth. The last row is a terminal snapshot with no further income or spending.
Investment returns apply to opening balances before annual contributions and
withdrawals. Cents are stored as integers; percentage calculations truncate fractional
cents toward zero. Simulated returns cannot reduce an asset below zero.

Scheduled withdrawals move money from a pool to household cash. Expenses use that
cash and, if necessary, withdraw additional assets in cash/brokerage/pre-tax/Roth/other
order. Real estate cannot automatically cover spending. Contributions use available
cash in priority order. Remaining cash is retained even after retirement or when no
asset pool exists. Funding reports include asset-funded expenses and actual withdrawal
shortfalls. Disabled rules exclude their target from the funding waterfall.

Home purchases are attempted in their scheduled year, and occur only if the entire
down payment is funded. The ledger then creates the property and mortgage together.
Property appreciation and annual mortgage interest start in the following year.
`annualMortgageAndTax` is the total annual payment budget; `annualPropertyCosts` is
its non-debt portion. Payments reduce accrued interest and principal; unpaid interest
remains a liability. Any remaining mortgage balance is due at the end of `loanTermYears`
(a balloon payment if the budget was insufficient). Property costs continue after payoff.
These are annual approximations, not monthly mortgage amortization. Templates cover
future purchases; backdated purchases are rejected because their outstanding debt
cannot be inferred from the purchase price.

A successful simulation funds every enabled expense in every projected year. A
missed contribution or scheduled withdrawal is reported separately and does not itself
fail a run if expenses can still be paid. Zero remaining wealth can be successful;
positive illiquid property does not hide an expense shortfall. Consolidated success
requires every household to succeed; households do not automatically transfer money
to one another.

Consolidated percentiles are computed from joint simulated outcomes, rather than
summing household percentiles. `simulatePlan` accepts a seed and a `returnCorrelation`
(default 0.75): pool return shocks combine a common annual market shock with an
independent shock. This is a simplified, configurable correlation assumption rather
than an asset-class covariance model. All households must share the plan's base year.
After a household's terminal year its balance is carried forward unchanged in the
consolidated chart, without assuming further income, spending, growth, or estate transfers.

The older `ForecastProfile` retirement helpers are independent real-dollar calculators:
their fixed savings and spending inputs are in today's dollars, so their returns are
inflation-adjusted. Their outputs should not be combined with the nominal household ledger.

Run checks with:

```sh
./gradlew :forecast:desktopTest :forecast:compileKotlinWasmJs :forecast:spotlessCheck
```

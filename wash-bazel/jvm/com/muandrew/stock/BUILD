load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "stock",
    srcs = [
        "Lot.kt",
        "LotIdentifier.kt",
        "LotSnapshot.kt",
        "Transaction.kt",
        "TransactionId.kt",
        "World.kt",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//jvm/com/muandrew/money",
        "//jvm/com/muandrew/stock/time",
    ],
)

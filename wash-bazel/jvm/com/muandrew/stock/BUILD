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
    plugins = [
        "//:moshi-kotlin-codegen",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//jvm/com/muandrew/money",
        "//jvm/com/muandrew/stock/time",
        "@maven//:com_squareup_moshi_moshi",
    ],
)

kt_jvm_test(
    name = "test",
    srcs = ["WorldTest.kt"],
    test_class = "com.muandrew.stock.WorldTest",
    deps = [
        ":stock",
        "@maven//:junit_junit",
    ],
)

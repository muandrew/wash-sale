load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "default",
    srcs = [
        "Lot.kt",
        "LotReference.kt",
        "LotValue.kt",
        "RawInput.kt",
        "Transaction.kt",
        "TransactionReference.kt",
        "TransactionReport.kt",
    ],
    plugins = [
        "//:moshi-kotlin-codegen",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//kotlin/com/muandrew/money",
        "//kotlin/com/muandrew/moshi/adapters",
        "//kotlin/com/muandrew/stock/time",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
)

kt_jvm_test(
    name = "test",
    srcs = ["LotValueTest.kt"],
    test_class = "com.muandrew.stock.model.LotValueTest",
    deps = [
        ":default",
        "@maven//:junit_junit",
    ],
)

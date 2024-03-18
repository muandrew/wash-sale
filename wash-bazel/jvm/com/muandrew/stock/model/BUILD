load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "default",
    srcs = [
        "Lot.kt",
        "LotReference.kt",
        "RawInput.kt",
        "ReportEvent.kt",
        "ShareValue.kt",
        "Transaction.kt",
        "TransactionReference.kt",
    ],
    plugins = [
        "//:moshi-kotlin-codegen",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//jvm/com/muandrew/money",
        "//jvm/com/muandrew/moshi/adapters",
        "//jvm/com/muandrew/stock/time",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
)

kt_jvm_test(
    name = "test",
    srcs = ["ShareValueTest.kt"],
    test_class = "com.muandrew.stock.model.ShareValueTest",
    deps = [
        ":default",
        "@maven//:junit_junit",
    ],
)

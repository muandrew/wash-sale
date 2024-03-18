load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "default",
    srcs = [
        "MoshiExt.kt",
        "StockTransactionReader.kt",
        "World.kt",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//jvm/com/muandrew/money",
        "//jvm/com/muandrew/stock/model:default",
        "//jvm/com/muandrew/stock/time",
        "@maven//:com_github_doyaaaaaken_kotlin_csv_jvm",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_adapters",
    ],
)

kt_jvm_test(
    name = "test",
    srcs = ["WorldTest.kt"],
    data = [
        "testdata/release.json",
        "testdata/sale.json",
    ],
    test_class = "com.muandrew.stock.world.WorldTest",
    deps = [
        ":default",
        "//jvm/com/muandrew/testtool",
        "@bazel_tools//tools/java/runfiles",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:junit_junit",
    ],
)

kt_jvm_test(
    name = "testReader",
    srcs = ["StockTransactionReaderTest.kt"],
    data = [
        "testdata/sample.csv",
        "testdata/test.json",
    ],
    test_class = "com.muandrew.stock.world.StockTransactionReaderTest",
    deps = [
        ":default",
        "//jvm/com/muandrew/stock/model:default",
        "//jvm/com/muandrew/testtool",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:junit_junit",
    ],
)

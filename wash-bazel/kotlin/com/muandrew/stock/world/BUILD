load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "default",
    srcs = [
        "LotDiffer.kt",
        "MoshiExt.kt",
        "StockTransactionReader.kt",
        "World.kt",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//kotlin/com/muandrew/csv:default",
        "//kotlin/com/muandrew/money",
        "//kotlin/com/muandrew/stock/model:default",
        "//kotlin/com/muandrew/stock/time:default",
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
        "//kotlin/com/muandrew/testtool",
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
        "//kotlin/com/muandrew/stock/model:default",
        "//kotlin/com/muandrew/testtool",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:junit_junit",
    ],
)

kt_jvm_test(
    name = "testLotDiffer",
    srcs = ["LotDifferTest.kt"],
    data = glob(["testdata/lot/*.*"]),
    test_class = "com.muandrew.stock.world.LotDifferTest",
    deps = [
        ":default",
        "//kotlin/com/muandrew/stock/model:default",
        "//kotlin/com/muandrew/testtool",
        "@maven//:junit_junit",
    ],
)

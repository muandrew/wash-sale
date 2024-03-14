load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_test")

kt_jvm_binary(
    name = "cli",
    srcs = ["StockCli.kt"],
    main_class = "com.muandrew.stock.cli.StockCli",
    deps = [
        "//jvm/com/muandrew/moshi/adapters",
        "//jvm/com/muandrew/stock/world:default",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_adapters",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
)

kt_jvm_test(
    name = "test",
    srcs = ["StockCliTest.kt"],
    data = glob(["testdata/*"]),
    test_class = "com.muandrew.stock.cli.StockCliTest",
    deps = [
        ":cli",
        "//jvm/com/muandrew/stock/model:default",
        "//jvm/com/muandrew/testtool",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:junit_junit",
    ],
)

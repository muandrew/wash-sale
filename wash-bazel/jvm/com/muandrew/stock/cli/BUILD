load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_test")

kt_jvm_binary(
    name = "cli",
    srcs = ["StockCli.kt"],
    main_class = "com.muandrew.stock.cli.StockCli",
    deps = [
        "//jvm/com/muandrew/moshi/adapters",
        "//jvm/com/muandrew/stock",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_adapters",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
)

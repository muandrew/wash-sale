load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_test")

kt_jvm_binary(
    name = "cli",
    srcs = ["StockCli.kt"],
    main_class = "com.muandrew.stock.cli.StockCli",
    deps = ["//jvm/com/muandrew/stock"],
)

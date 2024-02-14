load("@rules_java//java:defs.bzl", "java_binary")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

java_binary(
    name = "runner",
    srcs = ["Runner.java"],
    main_class = "com.example.cmdline.Runner",
    deps = [ ":util"]
)

kt_jvm_library(
    name = "util",
    srcs = glob(["Util.kt"]),
)
use wash_sale::timeline::example;

fn main() {
    println!("Walking through stock events");

    match example("src/test.csv") {
        Ok(()) => {}
        Err(err) => {
            println!("Error encountered, {}", err)
        }
    }
}

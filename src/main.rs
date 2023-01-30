use wash_sale::timeline::example;

fn main() {
    println!("Hello, world!");

    match example("src/test.csv") {
        Ok(()) => {}
        Err(err) => {
            println!("Error encountered, {}", err)
        }
    }
}

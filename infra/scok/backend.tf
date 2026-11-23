terraform {
  backend "s3" {
    bucket       = "scok-tfstate-156084119806-eu-west-1"
    key          = "scok/terraform.tfstate"
    region       = "eu-west-1"
    encrypt      = true
    use_lockfile = true
  }
}
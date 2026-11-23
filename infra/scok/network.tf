// We use data to Look up existing information sans creating resources
// Output to expose retrieved values to the console
// Filters to narrow the search

// HCL relies on DOTs syntax for example
// data.aws_vpc.default.id means
// Get the id of the aws_vpc data source I labeled "default".


provider "aws" {
  region = "eu-west-1"
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

output "vpc_id" {
  value = data.aws_vpc.default.id
}

output "subnet_ids" {
  value = data.aws_subnets.default.ids
}
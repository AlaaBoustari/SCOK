//We Create a bucket so our terraform state lives in the cloud
// We enable git-like features using versioning
// a terraform resource is identified by type + label :D

provider "aws" {
  region = "eu-west-1"
}

resource "aws_s3_bucket" "terraform_state" {
  bucket = "scok-tfstate-156084119806-eu-west-1"

  lifecycle {
    prevent_destroy = true
  }

}

resource "aws_s3_bucket_versioning" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id


  versioning_configuration {
    status = "Enabled"
  }
}
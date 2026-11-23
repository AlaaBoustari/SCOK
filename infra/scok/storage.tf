data "aws_caller_identity" "current" {

}

resource "aws_s3_bucket" "originals" {
  bucket = "scok-${data.aws_caller_identity.current.account_id}-eu-west-1-data"
  force_destroy = true
  
}

resource "aws_s3_bucket" "thumbnails" {
  bucket = "scok-thumbnail-${data.aws_caller_identity.current.account_id}-eu-west-1-data"
  force_destroy = true

}

resource "aws_s3_bucket_versioning" "originals" {
  bucket = aws_s3_bucket.originals.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "attachments" {
  for_each = {
    originals  = aws_s3_bucket.originals.id
    thumbnails = aws_s3_bucket.thumbnails.id
  }

  bucket = each.value

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_cors_configuration" "originals" {
  bucket = aws_s3_bucket.originals.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["PUT"]
    allowed_origins = concat(
      var.browser_origins,
      ["http://${aws_lb.scok.dns_name}"]
    )
    max_age_seconds = 3600
  }
}
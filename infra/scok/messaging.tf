resource "aws_sqs_queue" "thumbnail_dlq" {
  name                      = "ThumbnailDLQ"
  message_retention_seconds = 1209600
}

resource "aws_sqs_queue" "thumbnails" {
  name                       = "ThumbnailQueue"
  visibility_timeout_seconds = 60
  message_retention_seconds  = 172800
  receive_wait_time_seconds  = 20

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.thumbnail_dlq.arn
    maxReceiveCount     = 3
  })
}

resource "aws_sqs_queue_policy" "s3_notifications" {
  queue_url = aws_sqs_queue.thumbnails.url

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "s3.amazonaws.com"
      }
      Action   = "sqs:SendMessage"
      Resource = aws_sqs_queue.thumbnails.arn

      Condition = {
        ArnEquals = {
          "aws:SourceArn" = aws_s3_bucket.originals.arn
        }
        StringEquals = {
          "aws:SourceAccount" = data.aws_caller_identity.current.account_id
        }
      }
    }]
  })
}

resource "aws_s3_bucket_notification" "originals" {
  bucket = aws_s3_bucket.originals.id

  queue {
    queue_arn     = aws_sqs_queue.thumbnails.arn
    events        = ["s3:ObjectCreated:*"]
    filter_prefix = "originals/rooms/"
  }

  depends_on = [aws_sqs_queue_policy.s3_notifications]
}
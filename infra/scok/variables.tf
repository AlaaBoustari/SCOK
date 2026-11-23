variable "cognito_user_pool_id" {
  type = string
}

variable "cognito_client_id" {
  type = string
}

variable "cognito_client_secret_arn" {
  type = string
}

variable "browser_origins" {
  type = list(string)
  default = [
    "http://localhost:8080",
    "http://127.0.0.1:8080"
  ]
}

variable "image_tag" {
  type    = string
  default = "v1"
}
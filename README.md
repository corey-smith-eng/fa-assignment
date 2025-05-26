# FA Portfolio Transaction Report API

A Spring Boot REST API that connects to FA Solutions' GraphQL backend to generate CSV reports of investment portfolio transactions

---

##  Features

- Connects to FA Platform via secure OAuth2
- Supports filtering by:
    - `portfolioId` (required) (singular id)
    - `startDate`, `endDate` (optional, ISO-8601)
    - `targetCurrency` (default: USD, limited to 10 major currencies ["USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD"])
- Generates two CSV formats:
    - **Raw**: machine-readable (the main task requirements)
    - **Summary**: human-readable, with formatted numbers and conditional logic (can be reached with `pretty=true` RequestParam)
- Handles token refreshing and caching
- Includes basic HTTP authentication for access
- Binary `pretty=true` if you would 

---

## Example Request

```http

# For the minimum requirments 
GET /report?portfolioId=3&startDate=2023-01-01&endDate=2023-12-31
Authorization: Basic (base64-encoded credentials)

# For the summary report with extra processed data 
GET /report?portfolioId=3&startDate=2023-01-01&endDate=2023-12-31&pretty=true&targetCurrency=EUR
Authorization: Basic (base64-encoded credentials)


```

## Requirements
Java 17+

Maven 3.9.9+

Internet access to reach FA’s tryme.fasolutions.com API

## Configuration

Only set up needed is to create a ***application-secret.properties*** config file to pass the credentials into
```editorconfig
    fa.api.username=<replace this>
    fa.api.password=<replace this>
    app.auth.username=<replace this (Ill send credentials over email)>
    app.auth.password=<replace this (Ill send credentials over email)>
```

## Tests
mvn test

## Project structure
```src
├── main
│   ├── controller               # REST endpoint
│   ├── service                  # Business logic, CSV generation
│   ├── client                   # GraphQL & OAuth2 client
│   ├── model                    # FlatTransaction
│   ├── config                   # Security config
├   ├── Auth                     # Manages token lifecycle
├   ├── FaReportApplication.java #Main entrypoint
├── test
│   └── FaReportApplicationTests.java #Tests
```



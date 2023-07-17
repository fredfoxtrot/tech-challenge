# Tech Challenge Repository

This repository contains the solution for the Tech Challenge. The challenge is to implement a campsite reservation system using Java and Spring Boot.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Testing](#testing)

## Overview

The Tech Challenge repository provides a campsite reservation system implemented in Java with Spring Boot. The system allows users to make reservations for a campsite, check availability, modify existing reservations, and cancel reservations. It follows the specified requirements and constraints outlined in the challenge.

## Features

- Reservation creation: Users can make reservations for the campsite by providing their email, full name, arrival date, and departure date.
- Availability check: Users can check the availability of the campsite for a specific date range.
- Reservation modification: Users can modify existing reservations, including changing the arrival or departure date.
- Reservation cancellation: Users can cancel their reservations, freeing up the campsite for others.
- Concurrency handling: The system is designed to handle concurrent reservation requests and ensures data consistency.

## Installation

To install and set up the campsite reservation system locally, follow these steps:

1. Clone the repository:

   ```bash
   git clone https://github.com/fredfoxtrot/tech-challenge.git

2. Navigate to the project directory:

   ```bash
   cd tech-challenge

3. Build the project using Maven:

   ```bash
   mvn clean install

4. Run The Application:

   ```bash
   mvn spring-boot:run

The campsite reservation system should now be up and running locally on http://localhost:8080.

## Usage
Once the campsite reservation system is running, you can interact with it using the following endpoints:

GET /availability: Retrieve the availability of the campsite for a specific date range.
POST /reservation: Make a reservation by providing the required information.
PUT /reservation/{reservationId}: Modify an existing reservation by providing the reservation ID and updated details.
DELETE /reservation/{reservationId}: Cancel an existing reservation by providing the reservation ID.
Make sure to refer to the API documentation or Swagger UI for detailed information on request payloads and response formats.

## Testing
The campsite reservation system includes automated tests to ensure the correctness of the implemented features. 

The tests cover various scenarios, including reservation creation, availability check, reservation modification, cancellation, and concurrency handling.

To run the tests, use the following command:

   ```bash
   mvn clean install

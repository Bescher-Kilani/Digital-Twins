# Digital Twin Submodel Instantiation Web Application

## Overview

A Digital Twin is a virtual representation of a physical object or process, enabling real-time observation, analysis, simulation, and optimization throughout its lifecycle.

This project simplifies the creation and management of Submodel-Templates for Digital Twins, reducing the complexity of existing tools such as the [AASX Package Explorer](https://github.com/eclipse-aaspe/package-explorer).

Our web-based application allows users to easily instantiate selected Submodel-Templates, specifically:

* Digital Nameplate for Industrial Equipment
* Carbon Footprint

Additionally, the application supports generic instantiation of any Submodel-Template from the official [IDTA repository](https://industrialdigitaltwin.org/content-hub/teilmodelle), ensuring that templates are always up to date.

---

## Project Background

This project was commissioned by the Fraunhofer-Institut für Optronik, Systemtechnik und Bildauswertung (IOSB) in Karlsruhe and developed as part of the university course "Praxis der Softwareentwicklung".
It was implemented by a team of four students with the goal of making Digital Twin modeling more accessible, intuitive, and efficient.

---

## Features

### Template Management

* Automatic retrieval of the latest Submodel-Templates from the IDTA repository.
* Always up to date without manual updates.

### User Management

* **Guest users**:

  * Create new models.
  * Add submodels from templates.
  * Download models in JSON or AASX format.
* **Registered users**:

  * Personal dashboard with all created models.
  * Publish models to the ModelHub for others to view and save.
  * Delete, publish, or unpublish models at any time.

### ModelHub Search

* Search models by name, description, author, tags, or published date.

---

## Getting Started

### Prerequisites

* [Docker](https://www.docker.com/get-started) installed on your system.

### Installation and Setup

1. Clone the repository:

   ```bash
   git clone <your-repo-url>
   cd <your-repo-folder>
   ```

2. Build the Docker container:

   ```bash
   docker-compose build
   ```

3. Start the application:

   ```bash
   docker-compose up -d
   ```

4. Stop and remove containers:

   ```bash
   docker-compose down
   ```

5. Update the application:

   ```bash
   docker-compose build
   docker-compose up -d
   ```

---

## Usage Example

1. **Create a Model**

   * Select a Submodel-Template from the template list.
   * Fill in the required fields in the form interface.

2. **Publish to ModelHub**

   * Logged-in users can make their model public for others to view and save.

3. **Search for Models**

   * Use the search function to find models by name, description, author, tags, or published date.

---

## Goal

The goal of this project is to make Digital Twin modeling efficient, accessible, and user-friendly by:

* Reducing the entry barrier for small teams and companies.
* Providing an intuitive interface without unnecessary complexity.
* Ensuring that current and relevant templates are always available.

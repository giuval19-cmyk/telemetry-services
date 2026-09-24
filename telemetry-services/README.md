# 🛰️ Drone Telemetry Core Services

A highly concurrent, resilient, and production-ready Spring Boot microservice designed to ingest, monitor, and manage real-time drone telemetry at scale. Built with **Java 21 Virtual Threads**, **MongoDB Outbox Pattern**, and **Resilience4j**, this service bridges real-time event streams with fault-tolerant distributed command publishing.

## 🚀 Key Features

* **High-Throughput Ingestion**: Consumes real-time position and telemetry updates from a dedicated RabbitMQ queue.
* **Virtual Threads Worker Pool**: Leverages Java 21 Virtual Threads to spin up lightweight, dedicated concurrent workers that continuously monitor individual drone positions and battery levels in real-time.
* **Automated Threshold Alerts**: Automatically detects boundary violations and critical battery drops, dispatching reactive operational commands to an outbound RabbitMQ exchange.
* **Transactional Outbox Pattern**: Implements the Outbox Pattern backed by MongoDB to ensure reliable, at-least-once message delivery without dual-write inconsistencies.
* **Fault-Tolerant Publishing**: Protected by **Resilience4j Circuit Breakers** and Publisher Confirms to gracefully handle network partitions or broker downtime.
* **Service Discovery**: Seamlessly integrates with Netflix Eureka for cloud-native orchestration.

---

## 🛠️ Tech Stack

* **Java 21** 
* **Spring Boot** 
* **Spring Data MongoDB** 
* **Resilience4j**
* **Spring Cloud Netflix Eureka Client**
* **CloudAMQP** 
* **Maven**

---

## 🚀 Getting Started

### Prerequisites
* Java Development Kit (JDK 21+)
* MongoDB instance / cluster
* RabbitMQ / CloudAMQP instance
* Netflix Eureka Service Registry
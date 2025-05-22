![image](https://github.com/user-attachments/assets/809cbf2e-0522-4ff8-8db6-f70df3d46944)


# Tally2SQL Exporter

**Tally2SQL Exporter** is an open-source Spring Boot application designed to **connect to multiple Tally ERP instances** (via IP:PORT), extract data concurrently using **multi-threading**, and export it to a central **SQL database** such as MySQL, PostgreSQL, or any other database supported by JDBC.

---

## 🚀 Features

- ⚡ **Multi-Tally Instance Support**: Connects to and synchronizes with multiple Tally servers.
- 🧵 **Multi-threaded Processing**: Efficiently backs up data from multiple sources in parallel.
- 💾 **Incremental Sync**: Optionally syncs only new or modified records.
- 🛠️ **Pluggable Database Support**: Easily switch between MySQL, PostgreSQL, SQLite, and others.
- 📅 **Scheduled Backups**: Built-in scheduler for periodic exports.
- 🔒 **Configurable & Lightweight**: Simple YAML or properties-based config, deployable anywhere.

---

## 🏗️ Architecture

```
+--------------------+      +--------------------+        +--------------------+
|  Tally @ IP:9000   |      |  Tally @ IP:9001   |  ....  |  Tally @ IP:90XX   |
+--------------------+      +--------------------+        +--------------------+
         │                          │                            │
         └────────────┬─────────────┴────────────┬────────---────┘
                      ▼                          ▼
               ┌────────────────────────────────────────────┐
               │          Tally2SQL Exporter (Spring Boot)  │
               └────────────────────────────────────────────┘
                        │             │            │
                        ▼             ▼            ▼
                  MySQL DB      PostgreSQL DB    Other DBs
```

---

## 📦 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/tally2sql-exporter.git
cd tally2sql-exporter
```

### 2. Package the Application

```bash
  mvn clean package -DskipTests
```

### 3. Build docker image the Application

```bash
java -jar target/tally2sql-exporter-0.0.1-SNAPSHOT.jar
sudo docker build -t tally-backup/t:latest -f Dockerfile .
```

---

## ⚙️ Docker compose Configuration

Edit the `application.yml` or `application.properties` file to configure:

```yaml
version: '3.3'
services:
  prod-tally-backup-service:
    image: YOUR_IMAGE_NAME
    environment:
      CSV_DUMP_PATH: /var/log/
      CUSTOM_DB_CONNECTION_STRING: jdbc:mariadb://1xx.xx.xx4:3xx6/
      CUSTOM_DB_PASSWORD: 5xxxxxxxxxL
      CUSTOM_DB_USERNAME: sxxxxxxeDB
      DB_BASE_NAME: backup_tally
      DB_CONNECTION_STRING: jdbc:mariadb://1xx.xx.xx4:3xx6/tally_config?allowLoadLocalInfile=true
      DB_PASSWORD: 5xxxxxxxxxL
      DB_USERNAME: sxxxxxxeDB
      PROFILE: prod
      TALLY_BASE_URL: http://1xxx.xxx.xx6:6xx2/
      TALLY_COMPANY_NAME: YOUR_COMPANY_NAME
      TALLY_FROM_DATE: '2023-04-01'
      TALLY_SERVER_IP: 10x.xxx.xx.xx6
      TALLY_SERVER_PORTS: 6xx1,6xx2,6xx3
      TALLY_TO_DATE: '2024-03-31'
    ports:
     - 7111:8080
    logging:
      driver: json-file
    deploy:
      resources:
        reservations:
          memory: 5000M
        limits:
          memory: 6000M

```

---

## 🗂 Output

Depending on configuration, the following tables will be populated in your SQL database:

- `ledgers`
- `vouchers`
- `stock_items`
- and more...

---

## 🤝 Contributing

1. Fork this repository
2. Create your feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "Add feature"`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a pull request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

---

## ⭐ Support

If you find this project useful, give it a ⭐ on GitHub and share it with the community!

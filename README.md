<a id="readme-top"></a>

<p align="center">
  <a href="https://github.com/TectHost/TChat5/graphs/contributors"><img src="https://img.shields.io/github/contributors/TectHost/TChat5?style=for-the-badge" alt="Contributors"></a>
  <a href="https://github.com/TectHost/TChat5/network/members"><img src="https://img.shields.io/github/forks/TectHost/TChat5?style=for-the-badge" alt="Forks"></a>
  <a href="https://github.com/TectHost/TChat5/stargazers"><img src="https://img.shields.io/github/stars/TectHost/TChat5?style=for-the-badge" alt="Stargazers"></a>
  <a href="https://github.com/TectHost/TChat5/issues"><img src="https://img.shields.io/github/issues/TectHost/TChat5?style=for-the-badge" alt="Issues"></a>
  <a href="https://github.com/TectHost/TChat5/blob/main/LICENSE"><img src="https://img.shields.io/github/license/TectHost/TChat5?style=for-the-badge" alt="License"></a>
</p>

<p align="center">
  <a href="https://www.oracle.com/java/"><img src="https://img.shields.io/badge/Java-25-blue?style=for-the-badge&logo=java&logoColor=white" alt="Java"></a>
  <a href="https://papermc.io/"><img src="https://img.shields.io/badge/PaperMC-API-orange?style=for-the-badge" alt="PaperMC"></a>
  <a href="https://maven.apache.org/"><img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white" alt="Maven"></a>
</p>

---

## About The Project

**TChat** is an advanced, fully modular chat management plugin for Minecraft servers running PaperMC.

It is designed to be lightweight, scalable, and highly configurable, allowing server owners to control every aspect of the chat system.

### Features

- Modular architecture (enable/disable modules independently)
- Advanced chat formatting system
- Permission-based color chat
- Multi-language support
- Config-driven behavior (YAML)
- Admin update notifications
- Optimized for performance

---

## Built With

- **Java 25**
- **PaperMC API**
- **Maven**

Repository: https://github.com/TectHost/TChat5

---

## Getting Started

### Prerequisites

- Java 25+
- A Minecraft server running **PaperMC**
- PlaceholderAPI

---

### Installation

1. Download the latest `.jar` from the releases page
2. Place it in your server's `plugins/` folder
3. Start or restart your server

---

#### Build from source

1. Clone the repository:

```bash
git clone https://github.com/TectHost/TChat5.git
```

2. Enter the project directory:

```bash
cd TChat5
```

3. Build with Maven:

```bash
mvn clean package
```

4. The compiled `.jar` will be in:

```
/target/
```

---

## Usage

After starting the server:

- Configuration files will be generated in `/plugins/TChat/`
- Edit `.yml` files to customize behavior
- Reload or restart the server after changes

---

## Contributing

Contributions are welcome.

1. Fork the repository
2. Create a branch (`feature/your-feature`)
3. Commit changes
4. Push to your branch
5. Open a Pull Request

---

## License

Distributed under the MIT License.  
See `LICENSE` for more information.

---

## Acknowledgments

- Developed by [Tect.host](https://tect.host/)
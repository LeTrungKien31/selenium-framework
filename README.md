LINK YOUTUBE
https://youtu.be/eUr_tVtY36A

# Selenium Framework - Lab 11

Automation testing framework sử dụng Selenium WebDriver + TestNG + POM.

## Yêu cầu
- Java 17+
- Maven 3.8+
- Chrome/Firefox

## Chạy local
```bash
mvn clean test -Dbrowser=chrome -DsuiteXmlFile=testng-smoke.xml
```

## CI/CD
Pipeline tự động chạy trên GitHub Actions khi push lên `main`.
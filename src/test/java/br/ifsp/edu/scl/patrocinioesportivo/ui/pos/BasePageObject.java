package br.ifsp.edu.scl.patrocinioesportivo.ui.pos;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class BasePageObject {
    protected final WebDriver driver;

    public BasePageObject(WebDriver driver) 
    {
        this.driver = driver;
    }

    public String titulo()
    {
        return driver.findElement(By.tagName("h1")).getText();
    }

    protected void aguardaTitulo(String titulo) 
    {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.textToBe(By.tagName("h1"), titulo));
    }

    protected WebElement aguardaElemento(By localizador) 
    {
        return new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(localizador));
    }

    protected void aguardaSumir(By localizador)
    {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.invisibilityOfElementLocated(localizador));
    }
}

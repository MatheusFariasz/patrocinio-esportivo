package br.ifsp.edu.scl.patrocinioesportivo.ui.pos;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ContratoPageObject extends BasePageObject 
{

    public ContratoPageObject(WebDriver driver)
    {
        super(driver);
        aguardaElemento(By.className("status"));
    }

    public String status()
    {
        return driver.findElement(By.className("status")).getText();
    }

    public void aprovar()
    {
        driver.findElement(By.xpath("//button[text()='Aprovar']")).click();
        
        aguardaElemento(By.xpath("//dialog//button[text()='Confirmar']")).click();
    }

    public String mensagemModal()
    {
        return aguardaElemento(By.cssSelector("dialog [role='alert']")).getText();
    }

    public String mensagemSucesso()
    {
        aguardaSumir(By.tagName("dialog"));
        return driver.findElement(By.className("success")).getText();
    }
}

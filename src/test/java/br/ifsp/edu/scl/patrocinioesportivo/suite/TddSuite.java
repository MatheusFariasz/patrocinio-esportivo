package br.ifsp.edu.scl.patrocinioesportivo.suite;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Testes desenvolvidos com TDD")
@SelectPackages({"br.ifsp.edu.scl.patrocinioesportivo.application", "br.ifsp.edu.scl.patrocinioesportivo.domain"})
@IncludeTags("TDD")
public class TddSuite {
}

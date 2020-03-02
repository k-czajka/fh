package pl.fhframework.app.menu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import pl.fhframework.app.config.DefaultApplicationConfigurer;
import pl.fhframework.app.config.FhNavbarConfiguration;
import pl.fhframework.core.logging.FlushableRollingFileAppender;
import pl.fhframework.core.logging.FhLogger;
import pl.fhframework.core.rules.builtin.FhUserUtils;
import pl.fhframework.core.security.model.IBusinessRole;
import pl.fhframework.core.uc.IUseCaseRefreshListener;
import pl.fhframework.core.uc.UseCase;
import pl.fhframework.core.util.LogUtils;
import pl.fhframework.core.util.StringUtils;
import pl.fhframework.ISystemUseCase;
import pl.fhframework.annotations.Action;
import pl.fhframework.event.EventRegistry;

import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@UseCase
//@SystemFunction(DemoSystemFunction.DEMO_PANEL_TOP)
public class NavbarUC implements INavbar, ISystemUseCase, IUseCaseRefreshListener {
    public static final String NAVBAR_CONTAINER_ID = "navbarForm";

    private NavbarForm.Model model = new NavbarForm.Model();
    private static final String BOOTSWATCH_THEME_URL = "https://bootswatch.com/4/%s/bootstrap.css";

    @Autowired
    private MenuService menuService;

    @Autowired
    private EventRegistry eventRegistry;

    @Value("${server.servlet.context-path:/}")
    private String contextRoot;

    @Value("${server.logout.path:logout}")
    private String logoutPath;

    @Value("${server.login.path:login}")
    private String loginPath;

    @Value("${fh.application.default.css}")
    private String defaultCss;

    @Value("${fh.application.menu.hide}")
    private boolean menuHidden;

    @Value("${fhframework.language.default:pl}")
    private String defaultLanguage;

    @Autowired
    private DefaultApplicationConfigurer configurer;

    @Autowired
    private FhUserUtils userUtils;

    private Map<String, String> cssUrls;

    @Override
    public void start() {
        model.setLogin(getUserSession().getSystemUser().getLogin());
        model.setMenuService(menuService);
        if (contextRoot.endsWith("/")) {
            model.setLogoutURL(contextRoot + logoutPath);
            model.setLoginURL(contextRoot + loginPath);
        } else {
            model.setLogoutURL(contextRoot + "/" + logoutPath);
            model.setLoginURL(contextRoot + "/" + loginPath);
        }
        model.setGuest(userUtils.userHasRole(IBusinessRole.GUEST));

        setLanguage(defaultLanguage);
        openDefaultStylesheet();

        if (menuHidden) {
            hideMenu();
        }

        showForm(NavbarForm.class, model);
    }

    private void openDefaultStylesheet() {
        cssUrls = configurer.getCssUrls();
        model.setCssIds(new ArrayList<>(cssUrls.keySet()));
        model.setFhCss(model.getCssIds().remove(FhNavbarConfiguration.FH_CSS));
        model.setDefaultCss(model.getCssIds().remove(FhNavbarConfiguration.BASE_CSS));

        if (Objects.equals(FhNavbarConfiguration.FH_CSS, defaultCss)) {
            openFhStylesheet();
        }
        else if (Objects.equals(FhNavbarConfiguration.BASE_CSS, defaultCss)) {
            closeAlternativeStylesheet();
        }
        else {
            openStylesheet(defaultCss);
        }
    }

    @Action
    public void downloadUserLog() {
        FhLogger.info(this.getClass(), FlushableRollingFileAppender.FLUSH_MESSAGE);
        URL log = LogUtils.getUserLogFile(this.getUserSession());
        eventRegistry.fireDownloadEvent(new UrlResource(log));
    }

    @Override
    public String getContainerId() {
        return NAVBAR_CONTAINER_ID;
    }

    @Action
    public void setLanguagePolish() {
        this.setLanguage(NavbarForm.Language.POLISH.getValue());
    }

    @Action
    public void setLanguageEnglish() {
        this.setLanguage(NavbarForm.Language.ENGLISH.getValue());
    }

    private void setLanguage(String lang) {
        getUserSession().setLanguage(getLocale(lang));
        this.model.setLanguage(lang);
    }

    @Action
    public void openFhStylesheet() {
        String style = contextRoot + (contextRoot.endsWith("/") ? "" : "/") + "css/fh.css";
        model.setAlternativeStylesheet(style);
        eventRegistry.fireStylesheetChangeEvent(style);
    }

    @Action
    public void openStylesheet(String id) {
        model.setAlternativeStylesheet(cssUrls.get(id));
        eventRegistry.fireStylesheetChangeEvent(model.getAlternativeStylesheet());
    }

    @Action
    public void closeAlternativeStylesheet() {
        model.setAlternativeStylesheet(null);
        eventRegistry.fireStylesheetChangeEvent(null);
    }

    @Action
    public void toggleMenu() {
        if (menuService.isHidden()) {
            menuService.show();
        } else {
            menuService.hide();
        }
    }

    private Locale getLocale(String languageTag) {
        if (!StringUtils.isNullOrEmpty(languageTag)) {
            return Locale.forLanguageTag(languageTag);
        } else {
            return Locale.getDefault();
        }
    }

    @Override
    public void doAfterRefresh() {
        if (menuService.isHidden()) {
            menuService.hide();
        } else {
            menuService.show();
        }
    }

    @Override
    public void openMenu() {
        throw new IllegalStateException("Not implemented.");
    }

    @Override
    public void closeMenu() {
        throw new IllegalStateException("Not implemented.");
    }

    @Override
    public void showMenu() {
        menuService.show();
    }

    @Override
    public void hideMenu() {
        menuService.hide();
    }
}

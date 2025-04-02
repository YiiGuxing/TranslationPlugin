(function () {
    var lastPluginVersion = "v3.7";
    var activeSponsors = [{
        name: "DeerAPI",
        logo: "deer_api.svg",
        description: {
            default: "AI大模型API聚合平台，一键调用500+模型，7折特惠，最新GPT4o、Grok3、Gemini2.5pro全支持！"
        },
        links: {
            default: "https://api.deerapi.com/",
        }
    },];

    var RESOURCES_BASE_URL = "";
    var __DEV__ = localStorage.getItem("dev") === "true";

    function gaPlugin(hook) {
        hook.beforeEach(function () {
            gtag('event', 'page_view', {
                page_path: window.location.hash,
                page_title: document.title
            });
        });
    }

    function plugin(hook, vm) {
        hook.init(function () {
            vm.titles = {
                "docs": {
                    "/": "文档",
                    "/en/": "Documentation",
                    "/ja/": "ドキュメント",
                    "/ko/": "문서",
                },
                "updates": {
                    "/": "更新",
                    "/en/": "Updates",
                    "/ja/": "更新",
                    "/ko/": "업데이트",
                },
                "changelog": {
                    "/": "更新日志",
                    "/en/": "Changelog",
                    "/ja/": "更新ログ",
                    "/ko/": "변경 로그",
                },
                "support": {
                    "/": "支持与赞助",
                    "/en/": "Support/Sponsor",
                    "/ja/": "サポートとスポンサー",
                    "/ko/": "지원 및 기부",
                },
                "faq": {
                    "/": "FAQ",
                    "/en/": "FAQ",
                    "/ja/": "FAQ",
                    "/ko/": "FAQ",
                },
            };

            resetTitle(vm);
            initPlugin(vm);
        });

        hook.mounted(function () {
            if (vm.route.query.compact) {
                vm.config.topMargin = 0;
                vm.customConfig.compact = true;
            }

            initNavBar(vm);
            updateSidebar(vm);
        });

        hook.beforeEach(function () {
            var title = getTitle(vm);
            vm.currentTitle = title;
            document.title = title;
        });

        hook.afterEach(function (html, next) {
            var output = resolveImagePath(vm, html);
            output = resolveHeading(output);
            output = resolveIFrame(vm, output);
            next(output);
        });

        hook.doneEach(function () {
            document.title = vm.currentTitle;
            updateLanguage(vm);
            updateNavBar(vm);
            updateSidebar(vm);
            updateSponsors(vm);
            startTyped();
        });
    }

    function initPlugin(vm) {
        var pathRegex = /^\/((en|ja|ko)\/)?/;
        Object.defineProperty(vm.config, "loadSidebar", {
            get: function () {
                var path = (vm.route.path || "").replace(pathRegex, "")
                if (!path || path === "docs") {
                    return "_sidebar.md"
                }
                if (path.indexOf("updates/") === 0) {
                    if (vm.route.query.compact && path.indexOf("updates/v") === 0) {
                        return path.substring(8) + "_sidebar.md"
                    }

                    return "_sidebar.md"
                }
                if (path === "updates") {
                    return "updates/_sidebar.md"
                }
                if (path.indexOf("tutorial/") === 0) {
                    return "_sidebar.md"
                }

                return false;
            }
        });

        vm.customConfig = {
            compact: false
        };
    }

    function getTitle(vm) {
        var title = "";
        var matchResults = /^(\/((en|ja|ko)\/)?)(docs|support|faq|(updates)(\/(v\d+\.\d+|changelog)?)?)$/.exec(vm.route.path);
        if (matchResults) {
            if (matchResults[7] === "changelog") {
                title = vm.titles.changelog[matchResults[1]];
            } else if (matchResults[7]) {
                title = matchResults[7];
            } else {
                title = (vm.titles[matchResults[5] || matchResults[4]] || {})[matchResults[1]];
            }
        }

        if (title) {
            title += " · ";
        }

        return title + "IntelliJ Translation Plugin";
    }

    function resetTitle(vm) {
        var title = "IntelliJ Translation Plugin";
        vm.currentTitle = title;
        document.title = title;
    }

    function initNavBar(vm) {
        initNavLinks(vm);
        initNavTranslations(vm);

        var nav = document.querySelector(".app-nav");
        if (nav && vm.customConfig.compact) {
            nav.classList.add("hide")
        }
    }

    function initNavLinks(vm) {
        var navLinks = document.querySelectorAll(".app-nav a[data-path]");
        for (var i = 0; i < navLinks.length; i++) {
            navLinks[i].onclick = function () {
                window.location.href = "#" + getLanguagePath(vm) + this.getAttribute("data-path");

                if (this.id === "nav-home") {
                    resetTitle(vm);
                    setTimeout(function () {
                        document.scrollingElement.scrollTop = 0;
                    }, 0);
                }
            }
        }
    }

    function initNavTranslations(vm) {
        var translationLink = document.querySelector("#nav-translation");
        var menu = document.querySelector(".nav-translations--menu");
        if (translationLink && menu) {
            translationLink.onmousedown = function (e) {
                e.stopPropagation();
            };
            translationLink.onclick = function (e) {
                menu.classList.toggle("show");
            }

            var helpTranslate = document.querySelector("#help-translate");
            menu.onmousedown = function (e) {
                e.stopPropagation();
            };
            menu.onclick = function (e) {
                var isHelpTranslate = helpTranslate && helpTranslate.contains(e.target);
                var isLanguageLink = !isHelpTranslate && e.target.tagName === "A";
                if ((isHelpTranslate || isLanguageLink) && menu.classList.contains("show")) {
                    menu.classList.remove("show");
                }
                if (isLanguageLink) {
                    switchLanguage(vm, e.target.lang)
                }
            };
            document.addEventListener("mousedown", function () {
                if (menu.classList.contains("show")) {
                    menu.classList.remove("show");
                }
            });
        }
    }

    function updateNavBar(vm) {
        var navBar = document.querySelector("nav.app-nav");
        if (!navBar) {
            return;
        }

        var result = /^\/((en|ja|ko)\/)?(docs|updates|support|faq)/.exec(vm.route.path);
        navBar.setAttribute("data-path", result && result[3] || "");
    }

    function updateSidebar(vm) {
        var showSidebar = /^\/((en|ja|ko)\/)?((docs|tutorial|updates)(\/.*)?)?$/.test(vm.route.path);
        var main = document.querySelector("main");
        main && main.classList.toggle("hide-sidebar", !showSidebar);

        if (!showSidebar) {
            return;
        }

        var matchResults = /^\/((en|ja|ko)\/)?updates(\/(.+))?$/.exec(vm.route.path);
        if (!matchResults) {
            return;
        }

        var version = matchResults[4] || lastPluginVersion;
        var versionItems = document.querySelectorAll(".sidebar-nav > ul > li");
        for (var i = 0; i < versionItems.length; i++) {
            versionItems[i].classList.toggle("active--imp", versionItems[i].innerText.startsWith(version));
        }
    }

    function shuffle(array) {
        for (let i = array.length - 1; i > 0; i--) {
            let j = Math.floor(Math.random() * (i + 1));
            [array[i], array[j]] = [array[j], array[i]];
        }
        return array;
    }

    function updateSponsors(vm) {
        var sponsors = document.querySelectorAll(".sponsor-container");
        if (!sponsors) {
            return;
        }

        var labels = {
            "/": "❤️成为赞助商",
            "/en/": "❤️Become a Sponsor",
            "/ja/": "❤️スポンサーになる",
            "/ko/": "❤️후원자가 되다",
        }

        var languagePath = getLanguagePath(vm);
        var becomeASponsorUrl = "#" + languagePath + "support?id=sponsor-translation-plugin";
        sponsors.forEach(function (container) {
            container.textContent = "";
            if (activeSponsors.length) {
                //shuffle(activeSponsors);
                for (var i = 0; i < activeSponsors.length; i++) {
                    var sponsor = activeSponsors[i];
                    var sponsorLink = document.createElement("a");
                    sponsorLink.classList.add("sponsor");
                    sponsorLink.target = "_blank";
                    sponsorLink.href = sponsor.links[languagePath] || sponsor.links.default;

                    var description = sponsor.description[languagePath] || sponsor.description.default;
                    if (description) {
                        sponsorLink.dataset.description = sponsor.description[languagePath] || sponsor.description.default;
                    }

                    var img = document.createElement("img");
                    img.src = "./img/sponsor/" + sponsor.logo;
                    img.alt = sponsor.name;

                    sponsorLink.appendChild(img);
                    container.appendChild(sponsorLink);
                }

                var becomeASponsorLabelLink = document.createElement("a");
                becomeASponsorLabelLink.classList.add("label-link");
                becomeASponsorLabelLink.href = becomeASponsorUrl;
                becomeASponsorLabelLink.innerText = labels[languagePath];
                if (vm.customConfig.compact) {
                    becomeASponsorLabelLink.target = "_blank";
                }
                container.appendChild(becomeASponsorLabelLink);
            } else {
                var becomeASponsorLink = document.createElement("a");
                becomeASponsorLink.classList.add("sponsor");
                becomeASponsorLink.href = becomeASponsorUrl;
                becomeASponsorLink.innerText = labels[languagePath];
                if (vm.customConfig.compact) {
                    becomeASponsorLink.target = "_blank";
                }

                container.appendChild(becomeASponsorLink);
            }
        })
    }

    function updateLanguage(vm) {
        var result = /^\/((en|ja|ko)\/)?(.*)/.exec(vm.route.path);
        var lang = result && result[2] || "zh"
        document.documentElement.setAttribute("lang", lang);
    }

    function getLanguagePath(vm) {
        var result = /^(\/((en|ja|ko)\/)?)/.exec(vm.route.path);
        return result && result[0] || "/"
    }

    function switchLanguage(vm, newLanguage) {
        if (newLanguage) {
            var replacement = "/$4";
            if (newLanguage !== "zh") {
                replacement = "/" + newLanguage + replacement;
            }
            var newPath = vm.route.path.replace(/^(\/((en|ja|ko)\/)?)(.*)/, replacement);
            var query = ""
            for (const queryKey in vm.route.query) {
                if (query) {
                    query += "&"
                }
                query += queryKey + "=" + encodeURI(vm.route.query[queryKey] || "");
            }
            if (query) {
                query = "?" + query;
            }
            window.location.href = "#" + newPath + query;
        }
    }

    function resolveImagePath(vm, html) {
        if (!html) {
            return html;
        }

        var baseUrl = __DEV__ ? "" : RESOURCES_BASE_URL;
        return html.replace(/<img src="(.+?)"(.*?)data-origin="(.+?)"(.*?)>/g, function replacer(match, p1, p2, p3, p4) {
            var src = (p3 || "").charAt(0) === "/" ? baseUrl + p3 : p3;
            return '<img src="' + src + '"' + p2 + p4 + '>';
        });
    }

    function resolveHeading(html) {
        if (!html) {
            return html;
        }

        try {
            var reg = /<(?<tag>h[1-6])(?<ha> .*?)??><a(?<aa> .*?)??>(?<txt>.*?)<\/a><\/\k<tag>>/g;
            return html.replace(reg, '<$<tag>$<ha>>$<txt><a$<aa>>#</a></$<tag>>');
        } catch (e) {
        }

        return html;
    }

    function resolveIFrame(vm, html) {
        if (!html || !window.$intellij) {
            return html;
        }

        try {
            var reg = /<iframe.*?((>\s*?<\/iframe)|\/)>/gi;
            return html.replace(reg, '');
        } catch (e) {
        }

        return html;
    }

    function startTyped() {
        window.typedInstances = window.typedInstances || [];
        window.typedInstances.forEach(function (typed) {
            typed.destroy();
        });

        window.typedInstances = [];
        document.querySelectorAll("[data-typed]").forEach(function (el) {
            if (!el.offsetParent) {
                return;
            }
            var target = el.dataset.typed;
            if (!target) {
                return;
            }
            window.typedInstances.push(new Typed(el, {
                stringsElement: '[data-typed-target="' + target + '"]',
                typeSpeed: 60,
                backSpeed: 10,
                cursorChar: '_',
                smartBackspace: true,
                loop: true,
            }));
        });
    }

    window.$docsify = {
        // name: "TranslationPlugin",
        onlyCover: true,
        auto2top: true,
        relativePath: true,
        executeScript: true,
        loadSidebar: false,
        maxLevel: 3,
        topMargin: 60,
        plugins: [plugin, gaPlugin],
        coverpage: ['/', '/en/', '/ja/', '/ko/'],
        fallbackLanguages: ['en', 'ja', 'ko'],
        nameLink: {
            '/en/': '#/en/',
            '/ja/': '#/ja/',
            '/ko/': '#/ko/',
            '/': '#/',
        },
        alias: {
            "((/(en|ja|ko))?)/docs": "$1/",
            "((/(en|ja|ko))?)/updates(/?)": "$1/updates/" + lastPluginVersion,
            "((/(en|ja|ko))?)/updates/updates/_sidebar.md": "$1/updates/_sidebar.md",
        },
    };


    var params = new URLSearchParams(window.location.search);
    window.$intellij = params.get("intellij") || params.has("intellij");

    var searchParams = new URLSearchParams(window.location.hash.split("?")[1]);
    if (searchParams.get('compact')) {
        document.body.classList.add("compact-mode");
        var nav = document.querySelector(".app-nav");
        nav && nav.classList.add("hide");
    }
})();
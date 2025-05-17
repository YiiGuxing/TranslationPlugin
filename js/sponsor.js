(function () {
  /**
   * @type {{
   *   id: string,
   *   name: string,
   *   logo: string,
   *   description: string | {[lang:string]: string, default: string}
   *   link: string | {[lang:string]: string, default: string}
   * }[]}
   */
  const activeSponsors = [];

  const shuffleSponsors = false;

  const defaultLanguage = "en";
  const labels = {
    "zh-CN": "❤️成为赞助商",
    "en": "❤️Become a Sponsor",
    "ja": "❤️スポンサーになる",
    "ko": "❤️후원자가 되다",
  };

  function normalizeLanguage(language) {
    const [lang, country] = language.split("-")
    if (lang === "zh") {
      return country === "CN" ? language : defaultLanguage;
    } else {
      return lang;
    }
  }

  function shuffle(array) {
    for (let i = array.length - 1; i > 0; i--) {
      let j = Math.floor(Math.random() * (i + 1));
      [array[i], array[j]] = [array[j], array[i]];
    }
    return array;
  }

  /**
   * @param {string} containerSelectors
   * @param [options]
   * @param {string} [options.language]
   * @param {string} [options.place]
   * @param {string} [options.becomeASponsorLinkTarget]
   */
  function install(containerSelectors, options) {
    const containers = document.querySelectorAll(containerSelectors);
    if (!containers) {
      return;
    }

    options = {...options};
    options.language = options.language ? normalizeLanguage(options.language) : defaultLanguage;

    const languagePath = options.language === "zh-CN" ? "" : ("/" + options.language);
    const becomeASponsorUrl = "/#" + languagePath + "/support?id=sponsor-translation-plugin";
    containers.forEach(function (container) {
      if (activeSponsors.length) {
        if (shuffleSponsors) {
          shuffle(activeSponsors);
        }

        for (let i = 0; i < activeSponsors.length; i++) {
          const sponsor = activeSponsors[i];
          const sponsorLink = document.createElement("a");
          sponsorLink.classList.add("sponsor");
          sponsorLink.target = "_blank";

          const link = typeof sponsor.link === "string"
            ? sponsor.link
            : (sponsor.link[options.language] || sponsor.link.default);
          sponsorLink.href = "/sponsor/click?id=" + sponsor.id +
            "&name=" + encodeURIComponent(sponsor.name) +
            "&url=" + encodeURIComponent(link) +
            (options.place ? "&place=" + encodeURIComponent(options.place) : "");

          const description = typeof sponsor.description === "string"
            ? sponsor.description
            : (sponsor.description[options.language] || sponsor.description.default);
          if (description) {
            sponsorLink.dataset.description = description;
          }

          const img = document.createElement("img");
          img.src = "/img/sponsor/" + sponsor.logo;
          img.alt = sponsor.name;

          sponsorLink.appendChild(img);
          container.appendChild(sponsorLink);
        }
      }

      const becomeASponsorLink = document.createElement("a");
      becomeASponsorLink.classList.add("sponsor", "become-a-sponsor", activeSponsors.length ? "not-empty" : "empty");
      becomeASponsorLink.href = becomeASponsorUrl;
      becomeASponsorLink.innerText = labels[options.language] || labels[defaultLanguage];
      if (options.becomeASponsorLinkTarget) {
        becomeASponsorLink.target = options.becomeASponsorLinkTarget;
      }

      container.appendChild(becomeASponsorLink);
    });
  }

  window.$sponsorManager = {
    activeSponsors,
    install,
  };
})();
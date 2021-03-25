window.onload = function () {
    var searchParams = new URLSearchParams(window.location.search);
    if (searchParams.get('dark')) document.querySelector("body").classList.add("dark");
};

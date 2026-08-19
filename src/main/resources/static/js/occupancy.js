(function () {
    var form = document.getElementById("occupancy-filter");
    if (!form) {
        return;
    }

    form.addEventListener("submit", function () {
        var select = form.querySelector("[name='facilityId']");
        if (select && !select.value) {
            select.removeAttribute("name");
        }
    });

    form.querySelectorAll("input, select").forEach(function (el) {
        el.addEventListener("change", function () {
            form.requestSubmit();
        });
    });
})();

(function () {
    var sport = document.getElementById("sportType");
    var options = document.getElementById("level-options");
    var hint = document.getElementById("level-hint");
    var legend = document.getElementById("levels-legend");
    var fieldset = document.getElementById("levels-fieldset");
    if (!sport || !options) {
        return;
    }

    function refreshLevels() {
        var selected = sport.value;
        var hasSport = selected !== "";
        if (hint) {
            hint.hidden = hasSport;
        }
        options.hidden = !hasSport;
        options.setAttribute("aria-hidden", hasSport ? "false" : "true");
        if (fieldset) {
            fieldset.setAttribute("aria-disabled", hasSport ? "false" : "true");
        }
        if (legend) {
            var chosen = sport.options[sport.selectedIndex];
            var group = chosen ? chosen.getAttribute("data-group-label") : "";
            legend.textContent = hasSport && group ? group : "Levels you coach";
        }
        options.querySelectorAll(".coach-level").forEach(function (row) {
            var match = hasSport && row.getAttribute("data-sport") === selected;
            row.hidden = !match;
            var box = row.querySelector("input[type='checkbox']");
            if (!box) {
                return;
            }
            box.disabled = !match;
            if (!match) {
                box.checked = false;
            }
        });
    }

    sport.addEventListener("change", refreshLevels);
    refreshLevels();
})();

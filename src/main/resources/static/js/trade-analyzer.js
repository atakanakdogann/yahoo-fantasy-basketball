$(document).ready(function () {
    console.log("Trade Analyzer JS Loaded (Tailwind Version).");

    var leagueId = null;
    var myTeamKey = null;
    var csrfHeader = null;
    var csrfToken = null;

    // CSRF Token Check
    var $csrfDiv = $('#csrf-token');
    if ($csrfDiv.length) {
        csrfHeader = $csrfDiv.data('csrf-header');
        csrfToken = $csrfDiv.data('csrf-token');
    }

    // Element Selectors
    var $seasonSelect = $('#season');
    var $leagueSelect = $('#league');
    var $myTeamSelect = $('#my-team-select');
    var $opponentTeamSelect = $('#opponent-team-select');
    var $playersToSend = $('#players-to-send');
    var $playersToReceive = $('#players-to-receive');

    var $fillerSearchInput = $('#filler-search-input');
    var $fillerSelect = $('#filler-player-select');
    var $addFillerToMyTeamBtn = $('#add-filler-to-my-team-btn');
    var $addFillerToOpponentBtn = $('#add-filler-to-opponent-btn');

    var $analyzeBtn = $('#analyze-trade-btn');
    var $errorMessage = $('#error-message');
    var $resultsCard = $('#results-card');
    var $tradeSelectionContainer = $('#trade-selection-container');
    var $startOverBtn = $('#start-over-btn');

    // --- 1. SEASON SELECTION ---
    $seasonSelect.on('change', function () {
        var seasonId = $(this).val();
        $leagueSelect.empty().append('<option disabled selected>Loading leagues...</option>');

        $.get("/seasons/" + seasonId + "/leagues", function (data) {
            $leagueSelect.empty().append('<option disabled selected>Select a League</option>');
            $.each(data, function (index, league) {
                $leagueSelect.append($('<option>', { value: league.id, text: league.name }));
            });
            $leagueSelect.prop('disabled', false);
        }).fail(function () {
            $leagueSelect.empty().append('<option disabled selected>Error loading leagues</option>');
        });
    });

    // --- 2. LEAGUE SELECTION ---
    $leagueSelect.on('change', function () {
        leagueId = $(this).val();
        console.log("League Selected: " + leagueId);

        $errorMessage.addClass('hidden');
        $resultsCard.addClass('hidden');
        $tradeSelectionContainer.removeClass('hidden'); // Ensure visible
        resetAllDropdowns();

        // Fetch Teams
        $.get("/leagues/" + leagueId + "/info", function (data) {
            var teams = data.teams;
            $myTeamSelect.empty().append('<option disabled selected>Select your team...</option>');
            $opponentTeamSelect.empty().append('<option disabled selected>Select opponent...</option>');

            $.each(teams, function (index, team) {
                var tKey = team.id || team.teamKey;
                $myTeamSelect.append($('<option>', { value: tKey, text: team.name }));
                $opponentTeamSelect.append($('<option>', { value: tKey, text: team.name }));
            });

            $myTeamSelect.prop('disabled', false);
            $opponentTeamSelect.prop('disabled', false);
        });

        // Fetch Initial Free Agents
        loadFreeAgents(leagueId, "");
        $fillerSearchInput.prop('disabled', false);
        $fillerSelect.prop('disabled', false);
    });

    // --- FREE AGENT SEARCH ---
    var searchTimeout;
    $fillerSearchInput.on('input', function () {
        var query = $(this).val();
        $fillerSelect.empty().append('<option disabled>Searching...</option>');

        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(function () {
            loadFreeAgents(leagueId, query);
        }, 500);
    });

    function loadFreeAgents(lId, query) {
        // Don't clear if typing, just update status? 
        // Actually best to show loading.
        $fillerSelect.prop('disabled', false);

        $.get("/leagues/" + lId + "/free-agents", { query: query }, function (data) {
            $fillerSelect.empty();

            if (!data || data.length === 0) {
                $fillerSelect.append('<option disabled>No players found</option>');
                disableFillerButtons(true);
            } else {
                $.each(data, function (index, player) {
                    var pName = player.name && player.name.full ? player.name.full : player.fullName;
                    $fillerSelect.append($('<option>', {
                        value: player.playerKey || player.player_key,
                        text: `${pName} (${player.editorialTeamAbbr} - ${player.displayPosition})`
                    }));
                });
                disableFillerButtons(false);
            }
        }).fail(function (xhr, status, error) {
            console.error("FA Error:", error);
            $fillerSelect.empty().append('<option disabled>Error loading players</option>');
            disableFillerButtons(true);
        });
    }

    function disableFillerButtons(state) {
        $addFillerToMyTeamBtn.prop('disabled', state);
        $addFillerToOpponentBtn.prop('disabled', state);
    }

    // --- TEAM SELECTIONS ---
    $myTeamSelect.on('change', function () {
        myTeamKey = $(this).val();
        fetchRoster(myTeamKey, $playersToSend);
        validateTrade();
    });

    $opponentTeamSelect.on('change', function () {
        var opponentTeamKey = $(this).val();
        fetchRoster(opponentTeamKey, $playersToReceive);
        validateTrade();
    });

    $playersToSend.on('change', validateTrade);
    $playersToReceive.on('change', validateTrade);

    // --- ADD FILLER PLAYER UTILITY ---
    function addFillerToDropdown($targetDropdown) {
        var selectedPlayerKey = $fillerSelect.val();
        var selectedOption = $fillerSelect.find('option:selected');
        var selectedPlayerText = selectedOption.text();

        if (!selectedPlayerKey || selectedPlayerKey === '') {
            alert('Please select a player first');
            return;
        }

        // Check duplicates in BOTH lists
        var existsInSend = $playersToSend.find('option[value="' + selectedPlayerKey + '"]').length > 0;
        var existsInReceive = $playersToReceive.find('option[value="' + selectedPlayerKey + '"]').length > 0;

        if (existsInSend || existsInReceive) {
            alert('This player is already involved in the trade');
            return;
        }

        $targetDropdown.append($('<option>', {
            value: selectedPlayerKey,
            text: selectedPlayerText + " (FA)",
            selected: true
        }));

        // Reset filler input? Optional.
        // $fillerSearchInput.val('');
        // validateTrade();

        // Trigger change to validate
        validateTrade();
    }

    $addFillerToMyTeamBtn.on('click', function () {
        addFillerToDropdown($playersToSend);
    });

    $addFillerToOpponentBtn.on('click', function () {
        addFillerToDropdown($playersToReceive);
    });

    // --- ANALYZE BUTTON ---
    $analyzeBtn.on('click', function () {
        $(this).prop('disabled', true).text('Analyzing...');
        $errorMessage.addClass('hidden');
        $resultsCard.addClass('hidden');

        var teamAPlayers = $playersToSend.val() || [];
        var teamBPlayers = $playersToReceive.val() || [];

        var requestBody = {
            teamAPlayerKeys: teamAPlayers,
            teamBPlayerKeys: teamBPlayers
        };

        var headers = {};
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }

        $.ajax({
            url: `/leagues/${leagueId}/team/${myTeamKey}/analyze-trade`,
            type: 'POST',
            contentType: 'application/json',
            headers: headers,
            data: JSON.stringify(requestBody),
            success: function (result) {
                displayResults(result);
                $analyzeBtn.prop('disabled', false).text('Analyze Trade');
                // HIDE Selection, SHOW Results
                $tradeSelectionContainer.addClass('hidden');
                $resultsCard.removeClass('hidden');
            },
            error: function (xhr, status, error) {
                console.error("Analysis Error:", xhr.responseText);
                var msg = (xhr.status === 403) ? "Security Error" : (xhr.responseText || error);
                $errorMessage.text('Error: ' + msg).removeClass('hidden');
                $analyzeBtn.prop('disabled', false).text('Analyze Trade');
            }
        });
    });

    // --- START OVER BUTTON ---
    $startOverBtn.on('click', function () {
        // Reset View
        $resultsCard.addClass('hidden');
        $tradeSelectionContainer.removeClass('hidden');

        // Clear Selected Players but keep teams
        $playersToSend.val([]);

        var myTeamK = $myTeamSelect.val();
        var oppTeamK = $opponentTeamSelect.val();

        if (myTeamK) fetchRoster(myTeamK, $playersToSend);
        if (oppTeamK) fetchRoster(oppTeamK, $playersToReceive);

        $errorMessage.addClass('hidden');
        validateTrade();

        // Scroll to top
        $('html, body').animate({ scrollTop: 0 }, 500);
    });

    function fetchRoster(teamKey, $dropdown) {
        $dropdown.prop('disabled', true);
        $dropdown.val([]); // Clear selections

        $.get(`/leagues/${leagueId}/team/${teamKey}/roster`, function (players) {
            $dropdown.empty();

            if (!players || players.length === 0) {
                $dropdown.append('<option disabled>No players found</option>');
            } else {
                $.each(players, function (index, player) {
                    if (!player || !player.playerKey) return;
                    var pName = player.name && player.name.full ? player.name.full : player.fullName;

                    $dropdown.append($('<option>', {
                        value: player.playerKey,
                        text: `${pName} (${player.editorialTeamAbbr} - ${player.displayPosition})`
                    }));
                });
            }
            $dropdown.prop('disabled', false);

        }).fail(function (xhr, status, error) {
            console.error("Roster Error:", error);
            $dropdown.empty().append('<option disabled>Error loading roster</option>');
            $dropdown.prop('disabled', false);
        });
    }

    function validateTrade() {
        var teamAPlayers = $playersToSend.val() || [];
        var teamBPlayers = $playersToReceive.val() || [];

        if (teamAPlayers.length === 0 && teamBPlayers.length === 0) {
            $errorMessage.text('Please select players to trade.').removeClass('hidden');
            $analyzeBtn.prop('disabled', true);
        } else if (teamAPlayers.length !== teamBPlayers.length) {
            $errorMessage.text('Player counts must be equal. Use "Add Filler Player" to balance.').removeClass('hidden');
            $analyzeBtn.prop('disabled', true);
        } else {
            $errorMessage.addClass('hidden');
            $analyzeBtn.prop('disabled', false);
        }
    }

    function displayResults(result) {
        var $tableBody = $('#results-table tbody');
        $tableBody.empty();
        $('#team-a-stats-table tbody').empty();
        $('#team-b-stats-table tbody').empty();

        var winRateChange = result.winRateAfter - result.winRateBefore;
        var summaryText = '';

        // Tailwind Colors
        if (winRateChange > 0.001) {
            summaryText = `<h3 class="text-xl font-bold text-emerald-400">Trade Won (Win Rate: ${result.winRateBefore.toFixed(3)} ➔ ${result.winRateAfter.toFixed(3)})</h3>`;
        } else if (winRateChange < -0.001) {
            summaryText = `<h3 class="text-xl font-bold text-red-400">Trade Lost (Win Rate: ${result.winRateBefore.toFixed(3)} ➔ ${result.winRateAfter.toFixed(3)})</h3>`;
        } else {
            summaryText = `<h3 class="text-xl font-bold text-slate-400">Neutral Trade (Win Rate: ${result.winRateBefore.toFixed(3)} ➔ ${result.winRateAfter.toFixed(3)})</h3>`;
        }
        $('#results-summary').html(summaryText);

        // Render Player Stats
        if (result.teamAPlayers) {
            result.teamAPlayers.forEach(p => appendPlayerRow('#team-a-stats-table tbody', p));
        }
        if (result.teamBPlayers) {
            result.teamBPlayers.forEach(p => appendPlayerRow('#team-b-stats-table tbody', p));
        }

        // Render Category Changes
        $.each(result.categoryChanges, function (categoryName, change) {
            var before = result.categoryTotalsBefore[categoryName] || 0;
            var after = result.categoryTotalsAfter[categoryName] || 0;

            var rowClass = 'hover:bg-slate-800/50 transition-colors';
            var changeText = change.toFixed(1);
            var changeClass = "font-medium";

            // Tailwind Color Logic
            if (change > 0) {
                changeClass = "text-emerald-400 font-bold";
                changeText = '+' + changeText;
            } else if (change < 0) {
                changeClass = "text-red-400 font-bold";
            } else {
                changeClass = "text-slate-500";
            }

            var row =
                `<tr class="${rowClass}">
                <td class="px-4 py-3 border-b border-slate-700/50 text-white font-medium">${categoryName}</td>
                <td class="px-4 py-3 border-b border-slate-700/50 text-slate-400">${before.toFixed(1)}</td>
                <td class="px-4 py-3 border-b border-slate-700/50 text-slate-400">${after.toFixed(1)}</td>
                <td class="px-4 py-3 border-b border-slate-700/50 ${changeClass}">${changeText}</td>
              </tr>`;
            $tableBody.append(row);
        });

        $resultsCard.removeClass('hidden');
        $('html, body').animate({
            scrollTop: $("#results-card").offset().top - 80
        }, 500);
    }

    function appendPlayerRow(selector, player) {
        var name = (player.name && player.name.full) ? player.name.full : (player.fullName || "Unknown");
        // Access nested stats list: player.playerStats -> stats (Array) -> wrapper -> stat
        var statsList = (player.playerStats && player.playerStats.stats) ? player.playerStats.stats : [];

        var gp = getStatVal(statsList, ["0", "GP"]); // 0 is usually GP
        var fg = getStatVal(statsList, ["5", "FG%"]);
        var ft = getStatVal(statsList, ["8", "FT%"]);
        var p3 = getStatVal(statsList, ["10", "3PTM", "3PT"]);
        var pts = getStatVal(statsList, ["12", "PTS"]);
        var reb = getStatVal(statsList, ["15", "REB", "TRB"]);
        var ast = getStatVal(statsList, ["16", "AST"]);
        var st = getStatVal(statsList, ["17", "ST", "STL"]);
        var blk = getStatVal(statsList, ["18", "BLK"]);
        var to = getStatVal(statsList, ["19", "TO"]);

        var row = `
            <tr class="hover:bg-slate-700/30 transition-colors">
                <td class="px-3 py-2 text-white font-medium truncate" title="${name}">${name}</td>
                <td class="px-2 py-2">${gp}</td>
                <td class="px-2 py-2 text-indigo-300">${fg}</td>
                <td class="px-2 py-2 text-indigo-300">${ft}</td>
                <td class="px-2 py-2">${p3}</td>
                <td class="px-2 py-2 font-bold">${pts}</td>
                <td class="px-2 py-2">${reb}</td>
                <td class="px-2 py-2">${ast}</td>
                <td class="px-2 py-2">${st}</td>
                <td class="px-2 py-2">${blk}</td>
                <td class="px-2 py-2 text-red-300">${to}</td>
            </tr>
        `;
        $(selector).append(row);
    }

    function getStatVal(statsList, ids) {
        if (!statsList || !Array.isArray(statsList)) return "-";

        var val = "-";
        $.each(statsList, function (i, wrapper) {
            // Check if wrapper has stat object
            if (wrapper.stat) {
                var s = wrapper.stat;
                // Check Stat ID matches (string comparison) or Display Name matches
                var idMatch = s.statId && ids.includes(s.statId.toString());
                var nameMatch = s.displayName && ids.includes(s.displayName);

                if (idMatch || nameMatch) {
                    val = s.value;
                    return false; // break loop
                }
            }
        });
        return val;
    }

    function resetAllDropdowns() {
        // Since we are not using selectpicker, we just clear and disable the native select
        $myTeamSelect.empty().prop('disabled', true);
        $opponentTeamSelect.empty().prop('disabled', true);
        $playersToSend.empty().prop('disabled', true);
        $playersToReceive.empty().prop('disabled', true);

        $fillerSearchInput.val('').prop('disabled', true);
        $fillerSelect.empty().prop('disabled', true);

        $analyzeBtn.prop('disabled', true);
        disableFillerButtons(true);
    }
});

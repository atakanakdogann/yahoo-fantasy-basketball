$(document).ready(function () {

  $('#season').on('change', function () {
    var seasonId = $(this).val();
    $('#league').empty().append('<option disabled="disabled" selected="selected">Loading leagues...</option>');
    $('#live-standings-table tbody').html('<tr class="bg-slate-800/30 text-slate-400 text-center"><td colspan="4">Select a League</td></tr>');

    if ($.fn.DataTable.isDataTable('#live-standings-table')) {
      $('#live-standings-table').DataTable().destroy();
    }

    $.get("/seasons/" + seasonId + "/leagues", function (data) {
      var $leagueDropdown = $('#league');
      $leagueDropdown.empty().append('<option disabled="disabled" selected="selected">Select a League</option>');
      $.each(data, function (index, league) {
        $leagueDropdown.append($('<option>', {
          value: league.id,
          text: league.name
        }));
      });
    });
  });

  var dataTableInstance = null;

  $('#league').on('change', function () {
    var leagueId = $(this).val();
    var $tableBody = $('#live-standings-table tbody');

    if ($.fn.DataTable.isDataTable('#live-standings-table')) {
      dataTableInstance.destroy();
    }

    $tableBody.html('<tr class="bg-slate-800/30 text-slate-400 text-center"><td colspan="4">Live Standings Calculating...</td></tr>');

    $.get("/leagues/" + leagueId + "/live-standings", function (data) {
      var teams = data;
      $tableBody.empty();
      if (!teams || teams.length === 0) {
        $tableBody.html('<tr class="bg-slate-800/30 text-slate-400 text-center"><td colspan="4">Data not found.</td></tr>');
        return;
      }

      var lastWinRateStr = "";
      var currentDisplayRank = 0;

      $.each(teams, function (index, team) {

        var rowClass = '';

        var won = team.totalCategoriesWon;
        var tied = team.totalCategoriesTied;
        var played = team.totalCategoriesPlayed;
        var lost = played - won - tied;
        var recordString = `${won} / ${Math.round(lost)} / ${tied}`;

        var winRateStr = team.winRate ? team.winRate.toFixed(3) : "0.000";
        if (winRateStr.startsWith("0")) {
          winRateStr = winRateStr.substring(1);
        }

        // Tie Handling: If Win Rate string matches previous, use same rank.
        // Otherwise use actual position (index + 1).
        if (index === 0 || winRateStr !== lastWinRateStr) {
          currentDisplayRank = index + 1;
        }
        lastWinRateStr = winRateStr;

        if (index === 0) { rowClass = 'bg-emerald-500/30 text-emerald-200'; }
        else if (index === 1) { rowClass = 'bg-emerald-500/20 text-emerald-200'; }
        else if (index === 2) { rowClass = 'bg-emerald-500/10 text-emerald-200'; }
        else if (index === teams.length - 1) { rowClass = 'bg-red-500/30 text-red-200'; }
        else if (index === teams.length - 2) { rowClass = 'bg-red-500/20 text-red-200'; }
        else if (index === teams.length - 3) { rowClass = 'bg-red-500/10 text-red-200'; }
        else { rowClass = 'bg-transparent text-slate-300'; }

        var row =
          `<tr class="${rowClass}">
            <td class="px-4 py-3">${currentDisplayRank}</td>
            <td class="px-4 py-3 font-medium text-white">
              ${team.name}
            </td>
            <td class="px-4 py-3">${recordString}</td>
            <td class="px-4 py-3 font-bold">${winRateStr}</td>
          </tr>`;

        $tableBody.append(row);
      });


      dataTableInstance = $('#live-standings-table').DataTable({
        "paging": false,
        "info": false,
        "autoWidth": false,
        "order": [[0, "asc"]]
      });
    });
  });
});

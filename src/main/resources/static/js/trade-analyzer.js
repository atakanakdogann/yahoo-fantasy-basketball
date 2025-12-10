$(document).ready(function () {
    console.log("Trade Analyzer JS Yüklendi ve Başladı.");

    var leagueId = null;
    var myTeamKey = null;
    var csrfHeader = null;
    var csrfToken = null;

    // CSRF Token Kontrolü
    var $csrfDiv = $('#csrf-token');
    if ($csrfDiv.length) {
        csrfHeader = $csrfDiv.data('csrf-header');
        csrfToken = $csrfDiv.data('csrf-token');
    } else {
        console.warn("CSRF Token bulunamadı!");
    }

    // Element Seçimleri
    var $seasonSelect = $('#season');
    var $leagueSelect = $('#league');
    var $myTeamSelect = $('#my-team-select');
    var $opponentTeamSelect = $('#opponent-team-select');
    var $playersToSend = $('#players-to-send');
    var $playersToReceive = $('#players-to-receive');
    var $fillerSelect = $('#filler-player-select');
    var $analyzeBtn = $('#analyze-trade-btn');
    var $errorMessage = $('#error-message');
    var $resultsCard = $('#results-card');

    // Arama zamanlayıcı değişkeni
    var searchTimer = null;

    // --- 1. SEZON SEÇİMİ ---
    $seasonSelect.on('change', function () {
        var seasonId = $(this).val();
        $leagueSelect.empty().append('<option disabled selected>Loading leagues...</option>').selectpicker('refresh');

        $.get("/seasons/" + seasonId + "/leagues", function (data) {
            $leagueSelect.empty().append('<option disabled selected>Select a League</option>');
            $.each(data, function (index, league) {
                $leagueSelect.append($('<option>', { value: league.id, text: league.name }));
            });
            $leagueSelect.prop('disabled', false).selectpicker('refresh');
        }).fail(function() {
            $leagueSelect.empty().append('<option disabled selected>Error loading leagues</option>').selectpicker('refresh');
        });
    });

    // --- 2. LİG SEÇİMİ ---
    // --- 2. LİG SEÇİMİ ---
$leagueSelect.on('change', function () {
    leagueId = $(this).val();
    console.log("Lig Seçildi: " + leagueId);
    
    $errorMessage.addClass('d-none');
    $resultsCard.addClass('d-none');
    resetAllDropdowns();
    
    // Takımları Getir
    $.get("/leagues/" + leagueId + "/info", function (data) {
        var teams = data.teams;
        $myTeamSelect.empty().append('<option disabled selected>Select your team...</option>');
        $opponentTeamSelect.empty().append('<option disabled selected>Select opponent...</option>');
        
        $.each(teams, function (index, team) {
            var tKey = team.id || team.teamKey;
            $myTeamSelect.append($('<option>', { value: tKey, text: team.name }));
            $opponentTeamSelect.append($('<option>', { value: tKey, text: team.name }));
        });
        
        $myTeamSelect.prop('disabled', false).selectpicker('refresh');
        $opponentTeamSelect.prop('disabled', false).selectpicker('refresh');
    });
    
    // Free Agentları Getir (Başlangıçta genel liste)
    loadFreeAgents(leagueId, "");
    
    // KRİTİK EKLEME: Filler select'i de aktif et
    $fillerSelect.prop('disabled', false).selectpicker('refresh');
});

    var searchTimer = null;
var lastSearchQuery = ""; // Son arama sorgusunu hatırla

$(document).on('keyup', '.bs-searchbox input', function() {
    var $input = $(this);
    var $dropdownDiv = $input.closest('.dropdown');
    var $select = $dropdownDiv.find('select');
    
    // Sadece "Filler Player" kutusuysa işlem yap
    if ($select.attr('id') === 'filler-player-select') {
        var query = $input.val().trim();
        
        // Aynı sorgu tekrar ediliyorsa çık
        if (query === lastSearchQuery) {
            return;
        }
        
        // Önceki zamanlayıcıyı iptal et
        if (searchTimer) {
            clearTimeout(searchTimer);
        }
        
        console.log("Tuş basıldı, bekleniyor... Query:", query);
        
        // 500ms bekle (kullanıcı yazmayı bitirsin)
        searchTimer = setTimeout(function() {
            lastSearchQuery = query; // Son sorguyu kaydet
            
            if (query.length >= 2) {
                console.log("BACKEND'E GÖNDERİLİYOR: '" + query + "'");
                loadFreeAgents(leagueId, query);
            } else if (query.length === 0) {
                console.log("Boş arama, varsayılan liste yükleniyor");
                loadFreeAgents(leagueId, "");
            }
        }, 500); // 300'den 500'e çıkardım
    }
});

function loadFreeAgents(lId, query) {
    console.log("loadFreeAgents çağrıldı - League:", lId, "Query:", query);
    
    // Loading göstergesi
    $fillerSelect.empty().append('<option disabled>Searching...</option>');
    $fillerSelect.prop('disabled', false);
    $fillerSelect.selectpicker('refresh');
    
    // Backend'e 'query' adıyla parametre gönderiyoruz
    $.get("/leagues/" + lId + "/free-agents", { query: query }, function (data) {
        
        console.log("Backend'den gelen sonuç sayısı:", data ? data.length : 0);
        
        $fillerSelect.empty(); // TEMİZLE
        
        if(!data || data.length === 0) {
            $fillerSelect.append('<option disabled>No players found</option>');
            $('#add-filler-btn').prop('disabled', true);
        } else {
            // Sonuçları ekle
            $.each(data, function (index, player) {
                var pName = player.name && player.name.full ? player.name.full : player.fullName;
                
                console.log("Oyuncu ekleniyor:", pName); // Her oyuncuyu logla
                
                $fillerSelect.append($('<option>', { 
                    value: player.playerKey || player.player_key,
                    text: `${pName} (${player.editorialTeamAbbr} - ${player.displayPosition})`
                }));
            });
            $('#add-filler-btn').prop('disabled', false);
        }
        
        $fillerSelect.prop('disabled', false);
        $fillerSelect.selectpicker('refresh');
        
    }).fail(function(xhr, status, error) {
        console.error("Free agent yükleme hatası:", error);
        $fillerSelect.empty().append('<option disabled>Error loading players</option>');
        $('#add-filler-btn').prop('disabled', true);
        $fillerSelect.prop('disabled', false);
        $fillerSelect.selectpicker('refresh');
    });
}

    // --- TAKIM SEÇİMLERİ ---
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

    // --- 4. FREE AGENT EKLEME (SORUN 2 ÇÖZÜMÜ - TAM ÇALIŞIR HAL) ---
    $('#add-filler-btn').on('click', function() {
        var selectedPlayerKey = $fillerSelect.val();
        var selectedOption = $fillerSelect.find('option:selected');
        var selectedPlayerText = selectedOption.text();
        
        if (!selectedPlayerKey || selectedPlayerKey === '') {
            alert('Please select a player first');
            return;
        }
        
        console.log("Filler ekleniyor: " + selectedPlayerText + " (Key: " + selectedPlayerKey + ")");

        // 1. Seçeneği Alıcı listesine ekle (zaten var mı kontrol et)
        var exists = $playersToReceive.find('option[value="' + selectedPlayerKey + '"]').length > 0;
        if (exists) {
            alert('This player is already added');
            return;
        }
        
        $playersToReceive.append($('<option>', { 
            value: selectedPlayerKey, 
            text: selectedPlayerText + " (FA)",
            selected: true  // Otomatik seç
        }));
        
        // 2. Bootstrap Select'i güncelle
        $playersToReceive.selectpicker('refresh');
        
        // 3. Filler seçimini temizle
        $fillerSelect.selectpicker('val', '');
        
        // 4. Trade validasyonu
        validateTrade();
        
        console.log("Filler başarıyla eklendi. Şu anki seçililer:", $playersToReceive.val());
    });

    // --- ANALİZ BUTONU ---
    $analyzeBtn.on('click', function() {
        $(this).prop('disabled', true).text('Analyzing... (Please wait)');
        $errorMessage.addClass('d-none');
        $resultsCard.addClass('d-none');

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
            success: function(result) {
                displayResults(result);
                $analyzeBtn.prop('disabled', false).text('Analyze Trade');
            },
            error: function(xhr, status, error) {
                console.error("Analiz hatası:", xhr.responseText);
                var msg = (xhr.status === 403) ? "Security Error" : (xhr.responseText || error);
                $errorMessage.text('Error: ' + msg).removeClass('d-none');
                $analyzeBtn.prop('disabled', false).text('Analyze Trade');
            }
        });
    });

    // --- 5. ROSTER ÇEKME (SORUN 3 ÇÖZÜMÜ - TAM DÜZELTME) ---
    function fetchRoster(teamKey, $dropdown) {
        console.log("Roster çekiliyor için takım: " + teamKey);
        
        // Dropdown'u devre dışı bırak ve mevcut SEÇİLİ değerleri temizle
        $dropdown.prop('disabled', true);
        $dropdown.selectpicker('val', []); // SEÇİMLERİ TEMİZLE
        $dropdown.selectpicker('refresh');
        
        $.get(`/leagues/${leagueId}/team/${teamKey}/roster`, function (players) {
            
            // KRİTİK: Önce tüm option'ları sil
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
            
            // Dropdown'u aktif et ve güncelle
            $dropdown.prop('disabled', false);
            $dropdown.selectpicker('refresh');
            
            console.log("Roster yüklendi, toplam oyuncu:", players.length);
            
        }).fail(function(xhr, status, error) {
            console.error("Roster yükleme hatası:", error);
            $dropdown.empty().append('<option disabled>Error loading roster</option>');
            $dropdown.prop('disabled', false);
            $dropdown.selectpicker('refresh');
        });
    }

    function validateTrade() {
        var teamAPlayers = $playersToSend.val() || [];
        var teamBPlayers = $playersToReceive.val() || [];

        console.log("Validasyon - Team A:", teamAPlayers.length, "Team B:", teamBPlayers.length);

        if (teamAPlayers.length === 0 && teamBPlayers.length === 0) {
            $errorMessage.text('Please select players to trade.').removeClass('d-none');
            $analyzeBtn.prop('disabled', true);
        } else if (teamAPlayers.length !== teamBPlayers.length) {
            $errorMessage.text('Player counts must be equal. Use "Add Filler Player" to balance.').removeClass('d-none');
            $analyzeBtn.prop('disabled', true);
        } else {
            $errorMessage.addClass('d-none');
            $analyzeBtn.prop('disabled', false);
        }
    }
    
    function displayResults(result) {
        var $tableBody = $('#results-table tbody');
        $tableBody.empty();
        
        var winRateChange = result.winRateAfter - result.winRateBefore;
        var summaryText = '';
        
        // Renkler ve Metinler
        if (winRateChange > 0.001) {
            summaryText = `<h3 class="text-success">Trade Won (Win Rate: ${result.winRateBefore.toFixed(3)} ➔ ${result.winRateAfter.toFixed(3)})</h3>`;
        } else if (winRateChange < -0.001) {
            summaryText = `<h3 class="text-danger">Trade Lost (Win Rate: ${result.winRateBefore.toFixed(3)} ➔ ${result.winRateAfter.toFixed(3)})</h3>`;
        } else {
            summaryText = `<h3 class="text-muted">Neutral Trade (Win Rate: ${result.winRateBefore.toFixed(3)} ➔ ${result.winRateAfter.toFixed(3)})</h3>`;
        }
        $('#results-summary').html(summaryText);
        
        $.each(result.categoryChanges, function(categoryName, change) {
            var before = result.categoryTotalsBefore[categoryName] || 0;
            var after = result.categoryTotalsAfter[categoryName] || 0;
            
            var rowClass = '';
            var changeText = change.toFixed(1);
            
            // Renklendirme Mantığı
            if (change > 0) {
                rowClass = 'bg-success-light text-success'; 
                changeText = '+' + changeText;
            } else if (change < 0) {
                rowClass = 'bg-danger-light text-danger'; 
            }
            
            // Büyük değişimler için vurgu
            if (change > 5) rowClass = 'bg-success-medium text-white fw-bold';
            if (change < -5) rowClass = 'bg-danger-medium text-white fw-bold';

            var row = 
              `<tr class="${rowClass}">
                <td>${categoryName}</td>
                <td>${before.toFixed(1)}</td>
                <td>${after.toFixed(1)}</td>
                <td class="fw-bold">${changeText}</td>
              </tr>`;
            $tableBody.append(row);
        });
        
        $resultsCard.removeClass('d-none');
        $('html, body').animate({
            scrollTop: $("#results-card").offset().top - 80
        }, 500);
    }
    
    function resetAllDropdowns() {
        $myTeamSelect.empty().prop('disabled', true).selectpicker('refresh');
        $opponentTeamSelect.empty().prop('disabled', true).selectpicker('refresh');
        $playersToSend.empty().prop('disabled', true).selectpicker('refresh');
        $playersToReceive.empty().prop('disabled', true).selectpicker('refresh');
        $fillerSelect.empty().prop('disabled', true).selectpicker('refresh');
        $analyzeBtn.prop('disabled', true);
    }
});
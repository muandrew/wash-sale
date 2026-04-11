#!osascript

-- Specify the path to your TSV file
set filePath to POSIX path of (choose file with prompt "Select the TSV file to read:" of type {"tsv"})

-- Read the file content
set tsvContent to readTsvFile(filePath)

tell application "TurboTax" to activate

-- Loop through tsvContent
loopThroughContent(tsvContent)

-- Handler to read TSV file
on readTsvFile(filePath)
    set fileContent to ""
    try
        set fileHandle to open for access filePath
        set fileContent to read fileHandle as «class utf8»
        close access fileHandle
    on error errMsg
        display dialog "Error reading file: " & errMsg
    end try
    return fileContent
end readTsvFile

-- Handler to parse TSV content into variables
on loopThroughContent(tsvContent)
    set AppleScript's text item delimiters to tab
    set tsvRows to paragraphs of tsvContent
    set myCounter to 1
    repeat with tsvRow in tsvRows
        set {quantity, acquiredDate, saleDate, gross, basis, wash} to words of tsvRow
        log("processing: " & myCounter & " acq: " & acquiredDate & "sale: " & saleDate & " gross: " & gross & " basis: " & basis & " wash: " & wash)
        processTsvRow(false, "Uber 90353T100", "short", quantity, acquiredDate, saleDate, gross, basis, wash)
        set myCounter to myCounter + 1
    end repeat
end loopThroughContent

-- Handler for a row of TSV content
on processTsvRow(dryRun, desc, longShort, quantity, acquiredDate, saleDate, gross, basis, wash)
    if dryRun then
        log("dry, not doing anything")
    else
        tell application "System Events"
            keystroke tab
            delay .1
            -- sales section 3 for short term 6 for long term
            repeat 6 times
                key code 125 -- down
                delay .1
            end repeat
            -- short term non covered
            key code 76 -- enter
            delay .1
            keystroke tab
            delay .1
            keystroke tab
            delay .1
            -- description
            keystroke desc
            delay .1
            keystroke tab
            delay .1
            -- date acquired
            keystroke acquiredDate
            delay .1
            keystroke tab
            delay .1
            -- date sold or disposed
            keystroke saleDate
            delay .1
            keystroke tab
            delay .1
            -- proceedes
            keystroke gross
            delay .1
            keystroke tab
            delay .1
            -- basis
            keystroke basis
            delay .1
            if wash > 0 then
                keystroke tab using {option down}
                delay .2
                keystroke tab using {option down}
                delay .2
                -- wash sale checkbox
                key code 49 -- space
                delay .5 -- loading new ui elements
                keystroke tab
                delay .1
                keystroke tab
                delay .1
                -- 1g
                keystroke wash
                repeat 4 times
                    keystroke tab
                    delay .1
                end repeat
                repeat 6 times
                    keystroke tab using {option down}
                    delay .1
                end repeat
                key code 49 -- space
            else 
                repeat 6 times
                    keystroke tab using {option down}
                    delay .1
                end repeat
                key code 49 -- space
            end if
            -- delay 7 -- 2nd page additional situation load
            display dialog "done loading additional situation" -- use dialog for manual control
            tell application "TurboTax" to activate
            delay .2
            keystroke tab using {option down}
            delay .2
            repeat 2 times
                keystroke tab using {shift down, option down}
                delay .2
            end repeat
            -- continue
            key code 49 -- space
            -- back to home screen
            -- delay 10 -- delay to load home
            display dialog "done loading home" -- use dialog for manual control
            tell application "TurboTax" to activate
            delay .2
            keystroke tab using {option down}
            delay .2
            repeat 12 times -- need 5 for first 25 -- need 8 for next 26+ -- need 9 for 51+ need 10 for 76+  need 11 need 12 and 13, on 8+ pages need 12???
                keystroke tab using {shift down, option down}
                delay .2
            end repeat
            -- add another
            key code 49 -- space
            -- delay 12 -- delay to load the new entry screen
            display dialog "done loading new entry" -- use dialog for manual control
            tell application "TurboTax" to activate
            delay .2
        end tell
    end if
end processTsvRow

-- https://eastmanreference.com/complete-list-of-applescript-key-codes
--how to tab tab
--tell application "System Events"
--    keystroke tab
--      delay .2 (sec)
--end tell
-- tell application "System Events"
--     key code 48
-- end tell


-- -- Combine the data into a single string separated by line breaks
-- set dataToPaste to secondColumnData as text

-- -- Open Sublime Text and paste the data
-- tell application "Sublime Text"
--     activate
--     tell application "System Events" to keystroke "v" using {command down}
-- end tell

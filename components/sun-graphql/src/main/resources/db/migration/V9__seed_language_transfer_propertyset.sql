-- V9 seeds the language-transfer property set read by the Niece-Scarlett
-- Discord bot's /lt command. Entries are configurable so the FAQ can change
-- at runtime without a redeploy.

INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values,
                                       configurable, status, createdat, lastupdatedat)
VALUES (gen_random_uuid(), 'NieceScarlett', 'language-transfer', 'faq.en',
        '{"title": "What is Language Transfer?", "intro": "Language Transfer is an audio series that teaches the basics of Modern Greek in a natural and easy-to-comprehend manner. It focuses on grammar and teaches useful vocabulary to prepare you for everyday conversations.\n\nIt''s highly encouraged to check it out, as it will help you build a very solid foundation to communicate in Greek.", "bulletsHeader": "The complete series can be found on:", "bullets": [{"label": "YouTube", "url": "https://www.youtube.com/watch?v=dHsgJkV9J30&list=PLeA5t3dWTWvtWkl4oOV8J9SMB7L9N9Ogt"}, {"label": "Soundcloud", "url": "https://soundcloud.com/languagetransfer/sets/complete-greek-more-audios"}, {"label": "Transcript (PDF)", "url": "https://static1.squarespace.com/static/5c69bfa4f4e531370e74fa44/t/5d03d32873f6f10001a364b5/1560531782855/COMPLETE+GREEK+-+Transcripts_LT.pdf"}], "resourcesChannel": "https://discord.com/channels/350234668680871946/359578025228107776/1132288734738522112", "outro": {"blurb": "The audio series follows the teacher (Mihalis) as he teaches a student useful grammatical constructions and how to form sentences naturally, allowing you to follow along by putting yourself in the student''s shoes", "resources": "More useful resources can be found in", "resourcesLink": "the resources channel", "resourcesContinued": ", notably in the pins, to help you advance your Greek level after Language Transfer."}, "footer": "Ανιψιά Σκαρλέτα FAQ"}',
        TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values,
                                       configurable, status, createdat, lastupdatedat)
VALUES (gen_random_uuid(), 'NieceScarlett', 'language-transfer', 'faq.el',
        '{"message": "Πούτσα!"}',
        TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

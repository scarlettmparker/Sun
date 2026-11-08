-- V67 widens key path and name columns to text so deeply nested release
-- folders (over 255 characters) can be stored.

ALTER TABLE public.dionysus_key_detail ALTER COLUMN keypath TYPE text;
ALTER TABLE public.dionysus_key_detail ALTER COLUMN name TYPE text;

ALTER TABLE public.dionysus_subtitles ALTER COLUMN keypath TYPE text;
ALTER TABLE public.dionysus_subtitles ALTER COLUMN subtitlekey TYPE text;

ALTER TABLE public.dionysus_torrent_job ALTER COLUMN targetkeypath TYPE text;
ALTER TABLE public.dionysus_torrent_job ALTER COLUMN scratchpath TYPE text;

ALTER TABLE public.dionysus_torrent_file ALTER COLUMN path TYPE text;

ALTER TABLE public.dionysus_magnet_detail ALTER COLUMN displayname TYPE text;

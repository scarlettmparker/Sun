/**
 * Returns the age in whole years for a birth date.
 *
 * @param birthDate birth date to calculate from
 * @param now reference date, defaults to now
 * @returns age in years
 */
export function getAge(birthDate: Date, now: Date = new Date()): number {
  let age = now.getFullYear() - birthDate.getFullYear();
  const hadBirthdayThisYear =
    now.getMonth() > birthDate.getMonth() ||
    (now.getMonth() === birthDate.getMonth() &&
      now.getDate() >= birthDate.getDate());
  if (!hadBirthdayThisYear) {
    age -= 1;
  }
  return age;
}

/**
 * Returns years and months of experience from a start date.
 *
 * @param startDate start of experience
 * @param now reference date, defaults to now
 * @returns years and months
 */
export function getExperience(
  startDate: Date,
  now: Date = new Date(),
): { years: number; months: number } {
  let totalMonths =
    (now.getFullYear() - startDate.getFullYear()) * 12 +
    (now.getMonth() - startDate.getMonth());
  if (now.getDate() < startDate.getDate()) {
    totalMonths -= 1;
  }
  if (totalMonths < 0) {
    totalMonths = 0;
  }
  const years = Math.floor(totalMonths / 12);
  const months = totalMonths % 12;
  return { years, months };
}

/**
 * Returns YoE rounded to nearest half year.
 *
 * @param startDate start of experience
 * @param now reference date, defaults to now
 * @returns years rounded to 0.5 increments (e.g. 1, 1.5)
 */
export function getExperienceYoE(
  startDate: Date,
  now: Date = new Date(),
): number {
  let totalMonths =
    (now.getFullYear() - startDate.getFullYear()) * 12 +
    (now.getMonth() - startDate.getMonth());
  if (now.getDate() < startDate.getDate()) {
    totalMonths -= 1;
  }
  if (totalMonths < 0) {
    totalMonths = 0;
  }
  const raw = totalMonths / 12;
  return Math.round(raw * 2) / 2;
}

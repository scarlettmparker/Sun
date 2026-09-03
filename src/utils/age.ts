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

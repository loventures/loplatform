/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import React from 'react';
import { CiVault } from 'react-icons/ci';

// IoHomeOutline corrected for size and weight
export const Home = () => (
  <svg
    stroke="currentColor"
    fill="currentColor"
    strokeWidth="0"
    viewBox="16 16 480 480"
    height="1em"
    width="1em"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      fill="none"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="24"
      d="M80 212v236a16 16 0 0016 16h96V328a24 24 0 0124-24h80a24 24 0 0124 24v136h96a16 16 0 0016-16V212"
    ></path>
    <path
      fill="none"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="24"
      d="M480 256L266.89 52c-5-5.28-16.69-5.34-21.78 0L32 256m368-77V64h-48v69"
    ></path>
  </svg>
);

// BsBullseye with a checkmark added to the center
export const Bullseye: React.FC<{ size?: string }> = ({ size = '1em' }) => (
  <svg
    stroke="currentColor"
    fill="currentColor"
    strokeWidth="0"
    viewBox="0 0 16 16"
    height={size}
    width={size}
    xmlns="http://www.w3.org/2000/svg"
  >
    <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"></path>
    <path d="M8 13A5 5 0 1 1 8 3a5 5 0 0 1 0 10zm0 1A6 6 0 1 0 8 2a6 6 0 0 0 0 12z"></path>
    <path d="M8 11a3 3 0 1 1 0-6 3 3 0 0 1 0 6zm0 1a4 4 0 1 0 0-8 4 4 0 0 0 0 8z"></path>
    <path d="m 9.6419109,6.4803023 a 0.375,0.375 0 0 1 0.5350001,0.525 l -1.9950001,2.495 a 0.375,0.375 0 0 1 -0.54,0.01 l -1.323,-1.323 a 0.375,0.375 0 1 1 0.53,-0.53 l 1.047,1.0465 1.7365,-2.2125 a 0.1335,0.1335 0 0 1 0.01,-0.011 z" />
  </svg>
);

// CiVault corrected for size
export const Vault: React.FC<{ size?: string }> = ({ size = '1em' }) => (
  <CiVault
    size={size}
    viewBox="3 3 18 18"
  />
);

// IoSearchOutline corrected for size
export const Search: React.FC<{ size?: string }> = ({ size = '1em' }) => (
  <svg
    stroke="currentColor"
    fill="currentColor"
    strokeWidth="0"
    viewBox="48 48 416 416"
    height={size}
    width={size}
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      fill="none"
      strokeMiterlimit="10"
      strokeWidth="24"
      d="M221.09 64a157.09 157.09 0 10157.09 157.09A157.1 157.1 0 00221.09 64z"
    ></path>
    <path
      fill="none"
      strokeLinecap="round"
      strokeMiterlimit="10"
      strokeWidth="32"
      d="M338.29 338.29L448 448"
    ></path>
  </svg>
);

// IoHourglassOutline corrected for size and weight
export const Hourglass: React.FC<{ size?: string }> = ({ size = '1em' }) => (
  <svg
    stroke="currentColor"
    fill="currentColor"
    strokeWidth="0"
    viewBox="32 32 448 448"
    height={size}
    width={size}
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      fill="none"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="20"
      d="M145.61 464h220.78c19.8 0 35.55-16.29 33.42-35.06C386.06 308 304 310 304 256s83.11-51 95.8-172.94c2-18.78-13.61-35.06-33.41-35.06H145.61c-19.8 0-35.37 16.28-33.41 35.06C124.89 205 208 201 208 256s-82.06 52-95.8 172.94c-2.14 18.77 13.61 35.06 33.41 35.06z"
    ></path>
    <path d="M343.3 432H169.13c-15.6 0-20-18-9.06-29.16C186.55 376 240 356.78 240 326V224c0-19.85-38-35-61.51-67.2-3.88-5.31-3.49-12.8 6.37-12.8h142.73c8.41 0 10.23 7.43 6.4 12.75C310.82 189 272 204.05 272 224v102c0 30.53 55.71 47 80.4 76.87 9.95 12.04 6.47 29.13-9.1 29.13z"></path>
  </svg>
);

// IoChatboxOutline corrected for size and weight
export const Feedback: React.FC<{ size?: string }> = ({ size = '1em' }) => (
  <svg
    stroke="currentColor"
    fill="currentColor"
    strokeWidth="0"
    viewBox="32 32 448 448"
    height={size}
    width={size}
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      fill="none"
      strokeLinejoin="round"
      strokeWidth="24"
      d="M408 64H104a56.16 56.16 0 00-56 56v192a56.16 56.16 0 0056 56h40v80l93.72-78.14a8 8 0 015.13-1.86H408a56.16 56.16 0 0056-56V120a56.16 56.16 0 00-56-56z"
    ></path>
  </svg>
);

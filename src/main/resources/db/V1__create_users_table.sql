CREATE TABLE users
(
    id BIG-SERIAL PRIMARY KEY,                -- Automatically incrementing ID
    address_line1 character varying(255),
    city_name character varying(255),
    customer_id character varying(255),
    email character varying(255),
    first_name character varying(255),
    last_name character varying(255),
    phone_number character varying(255),
    state_name character varying(255),
    user_id character varying(255),
    user_name character varying(255),
    zip_code character varying(255)
);
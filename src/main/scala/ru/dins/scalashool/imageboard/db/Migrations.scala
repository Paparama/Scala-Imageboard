package ru.dins.scalashool.imageboard.db

import cats.effect.Sync
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux

object Migrations {

  private val migration: Update0 =
    sql"""
      create table if not exists posts(
        id BIGSERIAL PRIMARY KEY NOT NULL,
        image_ids BIGINT[] not null,
        text text not null,
        created_at TIMESTAMP not null,
        references_responses BIGINT[] not null,
        references_from BIGINT[] not null,
        tread_id bigint
      );

      create table if not exists boards(
        id BIGSERIAL PRIMARY KEY NOT NULL,
        name text not null,
        UNIQUE(name)
      );

      create table if not exists treads(
        id BIGSERIAL PRIMARY KEY NOT NULL,
        name text not null,
        board_id bigint not null,
        last_msg_created_time TIMESTAMP,
        UNIQUE(name)
      );

      create table if not exists images(
        id BIGSERIAL PRIMARY KEY NOT NULL,
        path text not null,
        post_id bigint not null
      );

      create table if not exists post_references(
        id BIGSERIAL PRIMARY KEY NOT NULL,
        reference_to bigint not null,
        post_id bigint not null,
        text text
      );
       """.update

  def migrate[F[_]: Sync](xa: Aux[F, Unit]): F[Unit] = migration.run.void.transact(xa)

}
